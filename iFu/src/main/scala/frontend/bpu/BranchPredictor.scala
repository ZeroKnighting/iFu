package iFu.frontend

import chisel3.{RegNext, _}
import chisel3.util._
import iFu.common._
import iFu.frontend.FrontendUtils._

class PredictionInfo extends Bundle with HasBPUParameters{
  val taken = Output(Bool())

  val isBranch = Output(Bool())

  val isJal = Output(Bool())

  val predictedpc = Valid(UInt(vaddrBits.W))

}

class PredictionMeta extends Bundle with HasBPUParameters{
  val bimMeta  = Output(new BIMPredictMeta)
  val BTBMeta  = Output(new BTBPredictMeta)
  val tageMeta = Output(new TagePredictMeta)
  val uBTBMeta = Output(new UBTBPredictMeta)
}

class BranchPredictionBundle extends Bundle with HasBPUParameters{
  val pc = UInt(vaddrBits.W)
  val predInfos = Vec(fetchWidth, new PredictionInfo)
  val meta = Vec(fetchWidth,new PredictionMeta)
}

class BranchPredictionUpdate extends Bundle with HasBPUParameters {
  val btbMispredicts = UInt(fetchWidth.W)
  def isCommitUpdate = btbMispredicts === 0.U

  val pc            = UInt(vaddrBits.W)
  // Mask of instructions which are branches.
  // If these are not cfiIdx, then they were predicted not taken
  val brMask       = UInt(fetchWidth.W)
  // Which CFI was taken/mispredicted (if any)
  val cfiIdx       = Valid(UInt(log2Ceil(fetchWidth).W))
  // Was the cfi taken?
  val cfiTaken     = Bool()
  // Was the cfi mispredicted from the original prediction?
  val cfiMispredicted = Bool()
  // Was the cfi a br?
  val cfiIsBr     = Bool()
  // Was the cfi a jal/jalr?
  val cfiIsJal  = Bool()
  // Was the cfi a jalr
  val cfiIsJalr = Bool()
  //val cfi_is_ret  = Bool()

  val gHist = new GlobalHistory

  // What did this CFI jump to?
  val target        = UInt(vaddrBits.W)

  val meta          = Vec(fetchWidth , new PredictionMeta)
}

class BranchPredictionRequest extends CoreBundle
{
  val pc    = UInt(vaddrBits.W)
  val gHist = new GlobalHistory
}

class BranchPredictor extends Module with HasBPUParameters
{
    val io = IO(new Bundle {
        val f0req = Flipped(Valid(new BranchPredictionRequest))

        val resp = Output(new Bundle {
          val f1 = new BranchPredictionBundle
          val f2 = new BranchPredictionBundle
          val f3 = new BranchPredictionBundle
        })

        val f3fire = Input(Bool())

        val update = Input(Valid(new BranchPredictionUpdate))

    })


    val s0valid = io.f0req.valid
    val s1valid = RegNext(s0valid)
    val s2valid = RegNext(s1valid)
    val s3valid = RegNext(s2valid)

    val s0mask = fetchMask(io.f0req.bits.pc)
    val s1mask = RegNext(s0mask)
    val s2mask = RegNext(s1mask)
    val s3mask = RegNext(s2mask)

    val s0pc = io.f0req.bits.pc
    val s1pc = RegNext(s0pc)
    val s2pc = RegNext(s1pc)
    val s3pc = RegNext(s2pc)

    val s0update = io.update
    val s1update = RegNext(s0update)

    val faubtb = Module(new FaUBtbPredictior())
    val bim = Module(new BimPredictor)
    val btb = Module(new BTBPredictor)
    // val tage = Module(new TagePredictor)

    faubtb.io.s1update := s1update
    btb.io.s1update := s1update
    bim.io.s1update := s1update
    // tage.io.f1update := s1update

    // 基本的pc和使能位
    faubtb.io.s1valid := s1valid
    faubtb.io.s1pc := s1pc

    btb.io.s0valid := s0valid
    btb.io.s0pc := s0pc

    bim.io.s0valid := s0valid
    bim.io.s0pc := s0pc

    // tage.io.f1valid := s1valid
    // tage.io.f1pc := s1pc

    // f1接收faubtb输出结果
    for (w <- 0 until fetchWidth) {
      io.resp.f1.predInfos(w).taken := faubtb.io.s1taken(w)
      io.resp.f1.predInfos(w).isBranch := faubtb.io.s1br(w)
      io.resp.f1.predInfos(w).isJal := faubtb.io.s1jal(w)
      io.resp.f1.predInfos(w).predictedpc := faubtb.io.s1targs(w)
    }



    // f2以f1为基础，接收btb，bim的输出结果
    io.resp.f2.predInfos := RegNext(io.resp.f1.predInfos)


    for (w <- 0 until fetchWidth) {
        // bim预测taken（不存在命不命中的说法）覆盖f2的初值
        io.resp.f2.predInfos(w).taken := bim.io.s2taken(w)

        // 对于btb，当且仅当命中，结果的valid有效，才会把对应的结果覆盖f2的初值
        when(btb.io.s2targs(w).valid) {
            io.resp.f2.predInfos(w).isBranch := btb.io.s2br(w)
            io.resp.f2.predInfos(w).isJal := btb.io.s2jal(w)
            io.resp.f2.predInfos(w).predictedpc := btb.io.s2targs(w)

            // btb推测为taken为真当且仅当检测到jal指令，这时bim置信度显然没有必然跳转的jal高，取btb的taken
            when(btb.io.s2taken(w)) {
              io.resp.f2.predInfos(w).taken := btb.io.s2taken(w)
            }
        }
    }

    // f3以f2为基础，接收tage的输出结果
    io.resp.f3.predInfos := RegNext(io.resp.f2.predInfos)

    // 对于tage，还需要全局历史，以及前面f3taken传入之前的预测值作为参考，一并传入
    // tage.io.f1gHist := io.f1ghist
    // tage.io.previousf3Taken := RegNext(VecInit(io.resp.f2.map(_.taken)))


    // tage可以根据传入的初值，在内部判断命中与否，在内部自行选择好是否覆盖f3的初值
    // 传出来的值不需要像btb一样再次判断，直接覆盖初值即可
    // for(w <- 0 until bankWidth){
    //     io.resp.f3.predInfos(w).taken := tage.io.f3taken(w)
    // }

    // 最后收集五个计数器预测过程中产生的meta信息
    for (w <- 0 until fetchWidth) {
        io.resp.f3.meta(w).uBTBMeta := faubtb.io.s3meta(w)
        io.resp.f3.meta(w).bimMeta := bim.io.s3meta(w)
        io.resp.f3.meta(w).BTBMeta := btb.io.s3meta(w)
        io.resp.f3.meta(w).tageMeta := DontCare
        // io.resp.f3.meta(w).tageMeta := tage.io.f3meta(w)
    }

    io.resp.f1.pc := RegNext(io.f0req.bits.pc)
    io.resp.f2.pc := RegNext(io.resp.f1.pc)
    io.resp.f3.pc := RegNext(io.resp.f2.pc)

    // We don't care about meta from the f1 and f2 resps
    // Use the meta from the latest resp
    io.resp.f1.meta := DontCare
    io.resp.f2.meta := DontCare



    when (io.update.valid) {
        when (io.update.bits.cfiIsBr && io.update.bits.cfiIdx.valid) {
          assert(io.update.bits.brMask(io.update.bits.cfiIdx.bits))
        }
    }
}

