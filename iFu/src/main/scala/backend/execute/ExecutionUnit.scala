package backend.execute

import backend.memSystem.LSUExeIO
import chisel3._
import chisel3.util._
import iFu.backend.FUConst.FUC_SZ
import iFu.backend.{ALUUnit, BrResolutionInfo, BrUpdateInfo, DivUnit, FuncUnit, FuncUnitReq, MemAddrCalcUnit, PipelinedMulUnit, SupportedFuncs}
import iFu.common.{CoreBundle, CoreModule, MicroOp}
import iFu.frontend.GetPCFromFtqIO
import iFu.backend.FUConst._

import scala.collection.mutable.ArrayBuffer


class ExeUnitResp extends CoreBundle
{
    val uop = new MicroOp
    val data = Bits(xLen.W)
    val predicated = Bool() // Was this predicated off?
}

abstract class ExecutionUnit(
    val readsIrf         : Boolean       = false,
    val writesIrf        : Boolean       = false,
    val writesLlIrf      : Boolean       = false,
    val numBypassStages  : Int,
    val bypassable       : Boolean       = false, // TODO make override def for code clarity
    val alwaysBypassable : Boolean       = false,
    val hasMem           : Boolean       = false,
    val hasCSR           : Boolean       = false,
    val hasJmpUnit       : Boolean       = false,
    val hasAlu           : Boolean       = false,
    val hasMul           : Boolean       = false,
    val hasDiv           : Boolean       = false,
) extends CoreModule
{
    val io = IO(new Bundle {
        val fu_types = Output(Bits(FUC_SZ.W))

        val req      = Flipped(new DecoupledIO(new FuncUnitReq))

        val iresp    = if (writesIrf)   new DecoupledIO(new ExeUnitResp) else null
        val ll_iresp = if (writesLlIrf) new DecoupledIO(new ExeUnitResp) else null

        val bypass   = Output(Vec(numBypassStages, Valid(new ExeUnitResp)))
        val brupdate = Input(new BrUpdateInfo())


        // only used by the branch unit
        val brinfo     = if (hasAlu) Output(new BrResolutionInfo()) else null
        val get_ftq_pc = if (hasJmpUnit) Flipped(new GetPCFromFtqIO()) else null
//        val status     = Input(new freechips.rocketchip.rocket.MStatus())


        // only used by the mem unit
        val lsu_io = if (hasMem) Flipped(new LSUExeIO) else null
//        val bp = if (hasMem) Input(Vec(nBreakpoints, new BP)) else null
//        val mcontext = if (hasMem) Input(UInt(coreParams.mcontextWidth.W)) else null
//        val scontext = if (hasMem) Input(UInt(coreParams.scontextWidth.W)) else null

        // TODO move this out of ExecutionUnit
        val com_exception = if (hasMem) Input(Bool()) else null
    })

    if (writesIrf)   {
        io.iresp.bits.predicated := false.B
        assert(io.iresp.ready)
    }
    if (writesLlIrf) {
//        io.ll_iresp.bits.fflags.valid := false.B
        io.ll_iresp.bits.predicated := false.B
    }
    require (bypassable || !alwaysBypassable,
        "[execute] an execution unit must be bypassable if it is always bypassable")

    def supportedFuncUnits = {
        new SupportedFuncs(
            alu = hasAlu,
            jmp = hasJmpUnit,
            mem = hasMem,
            muldiv = hasMul || hasDiv,
            csr = hasCSR
        )
    }
}

class ALUExeUnit(
    hasJmpUnit     : Boolean = false,
    hasCSR         : Boolean = false,
    hasAlu         : Boolean = true,
    hasMul         : Boolean = false,
    hasDiv         : Boolean = false,
    hasIfpu        : Boolean = false,
    hasMem         : Boolean = false,
    hasRocc        : Boolean = false
) extends ExecutionUnit(
            readsIrf         = true,
            writesIrf        = hasAlu || hasMul || hasDiv,
            numBypassStages  =
                if (hasAlu && hasMul) 3 //TODO XXX p(tile.TileKey).core.imulLatency
                else if (hasAlu) 1 else 0,
            bypassable       = hasAlu,
            alwaysBypassable = hasAlu && !(hasMem || hasJmpUnit || hasMul || hasDiv || hasCSR || hasIfpu || hasRocc),
            hasCSR           = hasCSR,
            hasJmpUnit       = hasJmpUnit,
            hasAlu           = hasAlu,
            hasMul           = hasMul,
            hasDiv           = hasDiv,
            hasMem           = hasMem
) with CoreModule {


    val div_busy  = WireInit(false.B)

    // The Functional Units --------------------
    // Specifically the functional units with fast writeback to IRF
    val iresp_fu_units = ArrayBuffer[FuncUnit]()

    io.fu_types := Mux(hasAlu.B, FU_ALU, 0.U) |
            Mux(hasMul.B, FU_MUL, 0.U) |
            Mux(!div_busy && hasDiv.B, FU_DIV, 0.U) |
            Mux(hasCSR.B, FU_CSR, 0.U) |
            Mux(hasJmpUnit.B, FU_JMP, 0.U) |
            Mux(hasMem.B, FU_MEM, 0.U)

    // ALU Unit -------------------------------
    var alu: ALUUnit = null
    if (hasAlu) {
        alu = Module(new ALUUnit(
            isJmpUnit = hasJmpUnit,
            numStages = numBypassStages,
        ))
        alu.io.req.valid := (
                io.req.valid &&
                        (io.req.bits.uop.fuCode === FU_ALU ||
                         io.req.bits.uop.fuCode === FU_JMP ||
                         io.req.bits.uop.fuCode === FU_CSR)
        )

        alu.io.req.bits.uop      := io.req.bits.uop
        alu.io.req.bits.kill     := io.req.bits.kill
        alu.io.req.bits.rs1Data := io.req.bits.rs1Data
        alu.io.req.bits.rs2Data := io.req.bits.rs2Data
        alu.io.req.bits.predData := io.req.bits.predData
        alu.io.resp.ready := DontCare
        alu.io.brUpdate := io.brupdate

        iresp_fu_units += alu

        // Bypassing only applies to ALU
        io.bypass := alu.io.bypass

        // branch unit is embedded inside the ALU
        io.brinfo := alu.io.brInfo
        if (hasJmpUnit) {
            alu.io.getFtqPC <> io.get_ftq_pc
        }
    }


    // Pipelined, IMul Unit ------------------
    var imul: PipelinedMulUnit = null
    if (hasMul) {
        imul = Module(new PipelinedMulUnit)
        imul.io <> DontCare
        imul.io.req.valid         := io.req.valid && io.req.bits.uop.fu_code_is(FU_MUL)
        imul.io.req.bits.uop      := io.req.bits.uop
        imul.io.req.bits.rs1Data := io.req.bits.rs1Data
        imul.io.req.bits.rs2Data := io.req.bits.rs2Data
        imul.io.req.bits.kill     := io.req.bits.kill
        imul.io.brUpdate := io.brupdate
        iresp_fu_units += imul
    }

    // Div/Rem Unit -----------------------
    var div: DivUnit = null
    val div_resp_val = WireInit(false.B)
    if (hasDiv) {
        div = Module(new DivUnit)
        div.io <> DontCare
        div.io.req.valid           := io.req.valid && io.req.bits.uop.fu_code_is(FU_DIV) && hasDiv.B
        div.io.req.bits.uop        := io.req.bits.uop
        div.io.req.bits.rs1Data   := io.req.bits.rs1Data
        div.io.req.bits.rs2Data   := io.req.bits.rs2Data
        div.io.brUpdate            := io.brupdate
        div.io.req.bits.kill       := io.req.bits.kill

        // share write port with the pipelined units
        div.io.resp.ready := !(iresp_fu_units.map(_.io.resp.valid).reduce(_|_))

        div_resp_val := div.io.resp.valid
        div_busy     := !div.io.req.ready ||
                (io.req.valid && io.req.bits.uop.fu_code_is(FU_DIV))

        iresp_fu_units += div
    }

    // Mem Unit --------------------------
    if (hasMem) {
        require(!hasAlu)
        val maddrcalc = Module(new MemAddrCalcUnit)
        maddrcalc.io.req        <> io.req
        maddrcalc.io.req.valid  := io.req.valid && io.req.bits.uop.fu_code_is(FU_MEM)
        maddrcalc.io.brUpdate     <> io.brupdate
//        maddrcalc.io.status     := io.status
//        maddrcalc.io.bp         := io.bp
//        maddrcalc.io.mcontext   := io.mcontext
//        maddrcalc.io.scontext   := io.scontext
        maddrcalc.io.resp.ready := DontCare
        require(numBypassStages == 0)

        io.lsu_io.req := maddrcalc.io.resp

        io.ll_iresp <> io.lsu_io.iresp

    }

    // Outputs (Write Port #0)  ---------------
    if (writesIrf) {
        io.iresp.valid     := iresp_fu_units.map(_.io.resp.valid).reduce(_|_)
        io.iresp.bits.uop  := PriorityMux(iresp_fu_units.map(f =>
            (f.io.resp.valid, f.io.resp.bits.uop)).toSeq)
        io.iresp.bits.data := PriorityMux(iresp_fu_units.map(f =>
            (f.io.resp.valid, f.io.resp.bits.data)).toSeq)
        io.iresp.bits.predicated := PriorityMux(iresp_fu_units.map(f =>
            (f.io.resp.valid, f.io.resp.bits.predicated)).toSeq)

        // pulled out for critical path reasons
        // TODO: Does this make sense as part of the iresp bundle?
//        if (hasAlu) {
//            io.iresp.bits.uop.csrAddr := ImmGen(alu.io.resp.bits.uop.imm_packed, IS_I).asUInt
//            io.iresp.bits.uop.ctrl.csr_cmd := alu.io.resp.bits.uop.ctrl.csr_cmd
//        }
    }

    assert ((PopCount(iresp_fu_units.map(_.io.resp.valid)) <= 1.U && !div_resp_val) ||
            (PopCount(iresp_fu_units.map(_.io.resp.valid)) <= 2.U && (div_resp_val)),
        "Multiple functional units are fighting over the write port.")
}

