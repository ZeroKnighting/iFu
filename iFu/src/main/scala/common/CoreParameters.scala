package iFu.common

import chisel3._
import chisel3.util._
import iFu.common.Consts._

class BPUParameters {
    val localHistoryLength: Int  = 0
    val globalHistoryLength: Int = 0
    val numRasEntries: Int       = 0
    val bpdMaxMetaLength: Int    = 0
}

class ICacheParameters {
    val nBanks: Int       = 2
    val nSets: Int        = 64
    val nWays: Int        = 8
    val lineBytes:Int     = 64
    val indexBits: Int    = log2Ceil(nSets)
    val offsetBits : Int  = log2Ceil(lineBytes)
    val untagBits: Int    = indexBits + offsetBits
    val tagBits: Int      = 32 - untagBits
    val bankBytes: Int    = 4 * 4
    val banksPerLine: Int = lineBytes / bankBytes
    require(isPow2(banksPerLine))
}

class FrontendParameters{
    val fetchWidth: Int                = 0
    val fetchBytes: Int                = fetchWidth * 4
    val numFTQEntries: Int             = 0
    val numFetchBufferEntries: Int     = 0
    val iCacheParams: ICacheParameters = new ICacheParameters
    val bpdParams: BPUParameters       = new BPUParameters
    val bankWidth: Int                 = fetchWidth / iCacheParams.nBanks
    val instrBytes:Int                 = 4
}

class ROBParameters{
    val robAddrSz: Int = 0
    val retireWidth: Int = 0
    val numRobRows: Int = 0
    val numRobEntries:Int = 0
}

class LSUParameters{
    val numSTQEntries: Int = 32
    val numLDQEntries: Int = 32
    val ldqAddrSz    : Int = log2Ceil(numLDQEntries)
    val stqAddrSz    : Int = log2Ceil(numSTQEntries)
}
class DcacheParameters {
    val nRowBits = 256
    def nRowBytes = nRowBits / 8
    def nRowWords = nRowBits / 32
    val nSets = 64
    val nWays = 8
    val nMSHR = 8
    val nTLBEntries = 32
    def nOffsetBits = log2Ceil(nRowWords)
    def nIdxBits = log2Ceil(nSets)
    def nTagBits = 32 - nOffsetBits - nIdxBits
    val coreDataBits = 32
    val vaddrBits = 32
    def nBlockAddrBits = vaddrBits - nOffsetBits

    val memWidth = 2

    val nFirstMSHRs = 4
    val nSecondMSHRs = 8

    def IsKilledByBranch (brupdate: BrUpdateInfo, uop: MicroOp): Bool = {
        return maskMatch(brupdate.b1.mispredictMask, uop.brMask)
    }

    def maskMatch(msk1: UInt, msk2: UInt): Bool = (msk1 & msk2) =/= 0.U

    def getIdx(vaddr: UInt): UInt = vaddr(nOffsetBits + nIdxBits - 1, nOffsetBits)
    def getTag(vaddr: UInt): UInt = vaddr(vaddrBits - 1, vaddrBits - nTagBits)
    def getBlockAddr(vaddr: UInt): UInt = vaddr(vaddrBits - 1, nOffsetBits)

    def isStore(req : DCacheReq): Bool = req.uop.use_stq

}

trait HasCoreParameters {
    val resetPC: Int = 0x1c000000
    val xLen: Int = 32
    val vaddrBits: Int = xLen
    val paddrBits: Int = xLen
    val coreInstrBytes: Int = 4
    val coreInstrBits: Int = coreInstrBytes * 8
    val coreWidth = 4
    val memWidth = 2
    val maxBrCount: Int = 20
    val numLRegs: Int = 32
    val lregSz: Int = log2Ceil(numLRegs)
    val numPRegs: Int = 108
    val pregSz: Int = log2Ceil(numPRegs)
    val frontendParams: FrontendParameters = new FrontendParameters
    val robParameters: ROBParameters = new ROBParameters
    val lsuParameters: LSUParameters = new LSUParameters
    val dcacheParameters: DcacheParameters = new DcacheParameters
    val issueParams: Seq[IssueParams] = Seq(
        IssueParams(issueWidth = 1, numIssueSlots = 16, iqType = IQT_MEM.litValue, dispatchWidth = 1),
        IssueParams(issueWidth = 2, numIssueSlots = 16, iqType = IQT_INT.litValue, dispatchWidth = 1))
}


