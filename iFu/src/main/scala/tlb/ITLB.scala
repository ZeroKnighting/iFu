package iFu.tlb

import chisel3._
import chisel3.util._

import iFu.common._
import iFu.common.Consts._

class ITLBCsrContext extends CoreBundle {
    val asid_asid = UInt(10.W)

    val crmd_da = Bool()
    val crmd_pg = Bool()
    val crmd_datm = UInt(2.W)
    val crmd_plv = UInt(2.W)

    val dmw0_plv0 = Bool()
    val dmw0_plv3 = Bool()
    val dmw0_mat = UInt(2.W)
    val dmw0_pseg = UInt(3.W)
    val dmw0_vseg = UInt(3.W)

    val dmw1_plv0 = Bool()
    val dmw1_plv3 = Bool()
    val dmw1_mat = UInt(2.W)
    val dmw1_pseg = UInt(3.W)
    val dmw1_vseg = UInt(3.W)
}

class ITLBReq extends CoreBundle {
    val vaddr = UInt(vaddrBits.W)
}

// PIF PPI ADEF TLBR
class ITLBException extends CoreBundle {
    val xcpt_cause = UInt(CauseCode.causeCodeBits.W)
}

class ITLBResp extends CoreBundle {
    val paddr     = UInt(paddrBits.W)
    val exception = Valid(new ITLBException)
}

class L0ITLBEntry extends CoreBundle {
    val exist = Bool()
    val entry = new TLBEntry()
}
object L0ITLBEntry {
    def new_entry(entry: TLBEntry) = {
        val e = Wire(new L0ITLBEntry)
        e.exist := true.B
        e.entry := entry
        e
    }

    def fake_entry(vppn: UInt) = {
        val e = Wire(new L0ITLBEntry)
        e := DontCare
        e.exist           := false.B
        e.entry.meta.e    := true.B
        e.entry.meta.g    := true.B
        e.entry.meta.vppn := vppn
        e
    }
}

class ITLBIO extends CoreBundle {
    val itlb_csr_cxt = Input(new ITLBCsrContext)
    val req          = Flipped(Valid(new ITLBReq))
    val resp         = Output(new ITLBResp)
    val r_req        = Output(new TLBDataRReq)
    val r_resp       = Flipped(Valid(new TLBDataRResp))
}

class ITLB(num_l0_itlb_entries: Int = 2) extends CoreModule {
    val io = IO(new ITLBIO)

    val s_ready :: s_refill :: Nil = Enum(2)
    val state     = RegInit(s_ready)
    val state_nxt = WireInit(state)
    state := state_nxt

    // L0 ITLB
    val l0_entry = RegInit(VecInit(
        Seq.fill(num_l0_itlb_entries)(0.U.asTypeOf(new L0ITLBEntry))
    ))

    val csr_regs = RegNext(io.itlb_csr_cxt)
    val da_mode  =  csr_regs.crmd_da && !csr_regs.crmd_pg
    val pg_mode  = !csr_regs.crmd_da &&  csr_regs.crmd_pg

    val vaddr        = io.req.bits.vaddr
    val l0_hit_oh    = VecInit(l0_entry.map(
        e => e.entry.matches(vaddr(vaddrBits - 1, 13), csr_regs.asid_asid)
    ))
    val l0_hit       = l0_hit_oh.asUInt.orR
    val l0_hit_idx   = OHToUInt(l0_hit_oh)
    val l0_hit_entry = l0_entry(l0_hit_idx)

    // addr translation
    io.resp := 0.U.asTypeOf(new ITLBResp)
    when (vaddr(1, 0) =/= 0.U) {
        io.resp.exception.valid           := true.B
        io.resp.exception.bits.xcpt_cause := CauseCode.ADEF
    } .elsewhen (da_mode) {
        io.resp.paddr := vaddr
    } .elsewhen (pg_mode) {
        val dmw0_en = (
            (csr_regs.dmw0_plv0 && csr_regs.crmd_plv === 0.U) ||
            (csr_regs.dmw0_plv3 && csr_regs.crmd_plv === 3.U)
        ) && (vaddr(31, 29) === csr_regs.dmw0_vseg)
        val dmw1_en = (
            (csr_regs.dmw1_plv0 && csr_regs.crmd_plv === 0.U) ||
            (csr_regs.dmw1_plv3 && csr_regs.crmd_plv === 3.U)
        ) && (vaddr(31, 29) === csr_regs.dmw1_vseg)
        if (!FPGAPlatform) dontTouch(dmw0_en)
        if (!FPGAPlatform) dontTouch(dmw1_en)

        when (dmw0_en || dmw1_en) {
            io.resp.paddr           := Cat(
                Mux(dmw0_en, csr_regs.dmw0_pseg, csr_regs.dmw1_pseg), vaddr(28, 0)
            )
            io.resp.exception.valid := false.B
        } .otherwise {
            val entry = l0_hit_entry.entry
            val odd_even_page = Mux(entry.meta.ps === 12.U, vaddr(12), vaddr(21))
            val data = entry.data(odd_even_page)
            switch (state) {
                is (s_ready) {
                    when (!l0_hit) {
                        state_nxt := s_refill
                        io.resp.exception.valid           := true.B
                        io.resp.exception.bits.xcpt_cause := CauseCode.MINI_EXCEPTION_L0TLB_MISS
                    } .elsewhen (!l0_hit_entry.exist) {
                        io.resp.exception.valid           := true.B
                        io.resp.exception.bits.xcpt_cause := CauseCode.TLBR
                    } .otherwise {
                        when (!data.v) {
                            io.resp.exception.valid           := true.B
                            io.resp.exception.bits.xcpt_cause := CauseCode.PIF
                        } .elsewhen(csr_regs.crmd_plv > data.plv) {
                            io.resp.exception.valid           := true.B
                            io.resp.exception.bits.xcpt_cause := CauseCode.PPI
                        }
                    }
                }
                is (s_refill) {
                    io.resp.exception.valid           := true.B
                    io.resp.exception.bits.xcpt_cause := CauseCode.MINI_EXCEPTION_L0TLB_MISS
                }
            }
            io.resp.paddr := Mux(
                entry.meta.ps === 12.U,
                Cat(data.ppn, vaddr(11, 0)),
                Cat(data.ppn(paddrBits - 13, 9), vaddr(20, 0))
            )
        }
    }

    // access L1 TLB
    io.r_req.vaddr := RegNext(vaddr)
    val r_resp = RegNext(io.r_resp)

    val refill_vppn = RegNext(RegNext(vaddr(vaddrBits - 1, 13)))
    val refill_en   = RegNext(RegNext(io.req.valid && !l0_hit)) && (state === s_refill)
    val refill_idx  = RegInit(0.U(log2Ceil(num_l0_itlb_entries).W))
    refill_idx := refill_idx + refill_en
    if (!FPGAPlatform) dontTouch(refill_idx)

    when (refill_en) {
        l0_entry(refill_idx) := Mux(
            r_resp.valid,
            L0ITLBEntry.new_entry(r_resp.bits.entry),
            L0ITLBEntry.fake_entry(refill_vppn)
        )
        state_nxt := s_ready
    }
}
