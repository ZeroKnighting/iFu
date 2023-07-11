package iFu.common

import chisel3._
import chisel3.util._

trait MicroOpCode {
    val UOPC_SZ = 6.W
    val uopX            = BitPat.dontCare(UOPC_SZ)

    val uopNOP          = 0.U(UOPC_SZ)
    val uopADD          = 1.U(UOPC_SZ)
    val uopSUB          = 2.U(UOPC_SZ)
    val uopSLT          = 3.U(UOPC_SZ)
    val uopSLTU         = 4.U(UOPC_SZ)
    val uopNOR          = 5.U(UOPC_SZ)
    val uopAND          = 6.U(UOPC_SZ)
    val uopOR           = 7.U(UOPC_SZ)
    val uopXOR          = 8.U(UOPC_SZ)
    val uopLU12IW       = 9.U(UOPC_SZ)
    val uopADDIW        = 10.U(UOPC_SZ)
    val uopSLTI         = 11.U(UOPC_SZ)
    val uopSLTUI        = 12.U(UOPC_SZ)
    val uopPCADDI       = 13.U(UOPC_SZ)
    val uopPCADDU12I    = 14.U(UOPC_SZ)
    val uopANDN         = 15.U(UOPC_SZ)
    val uopORN          = 16.U(UOPC_SZ)
    val uopANDI         = 17.U(UOPC_SZ)
    val uopORI          = 18.U(UOPC_SZ)
    val uopXORI         = 19.U(UOPC_SZ)
    val uopMULW         = 20.U(UOPC_SZ)
    val uopMULHW        = 21.U(UOPC_SZ)
    val uopMULHWU       = 22.U(UOPC_SZ)
    val uopDIVW         = 23.U(UOPC_SZ)
    val uopMODW         = 24.U(UOPC_SZ)
    val uopDIVWU        = 25.U(UOPC_SZ)
    val uopMODWU        = 26.U(UOPC_SZ)
    val uopSLLIW        = 27.U(UOPC_SZ)
    val uopSRLIW        = 28.U(UOPC_SZ)
    val uopSRAIW        = 29.U(UOPC_SZ)
    val uopSLLW         = 30.U(UOPC_SZ)
    val uopSRLW         = 31.U(UOPC_SZ)
    val uopSRAW         = 32.U(UOPC_SZ)
    val uopJIRL         = 33.U(UOPC_SZ)
    val uopB            = 34.U(UOPC_SZ)
    val uopBL           = 35.U(UOPC_SZ)
    val uopBEQ          = 36.U(UOPC_SZ)
    val uopBNE          = 37.U(UOPC_SZ)
    val uopBLT          = 38.U(UOPC_SZ)
    val uopBGE          = 39.U(UOPC_SZ)
    val uopBLTU         = 40.U(UOPC_SZ)
    val uopBGEU         = 41.U(UOPC_SZ)
    val uopSYSCALL      = 42.U(UOPC_SZ)
    val uopBREAK        = 43.U(UOPC_SZ)
    val uopCSRRD        = 44.U(UOPC_SZ)
    val uopCSRWR        = 45.U(UOPC_SZ)
    val uopCSRXCHG      = 46.U(UOPC_SZ)
    val uopERTN         = 47.U(UOPC_SZ)
    val uopRDCNTIDW     = 48.U(UOPC_SZ)
    val uopRDCNTVLW     = 49.U(UOPC_SZ)
    val uopRDCNTVHW     = 50.U(UOPC_SZ)
    val uopIDLE         = 51.U(UOPC_SZ)
    val uopTLBSRCH      = 52.U(UOPC_SZ)
    val uopTLBRD        = 53.U(UOPC_SZ)
    val uopTLBWR        = 54.U(UOPC_SZ)
    val uopTLBFILL      = 55.U(UOPC_SZ)
    val uopINVTLB       = 56.U(UOPC_SZ)
    val uopCACOP        = 57.U(UOPC_SZ)
    val uopPRELD        = 58.U(UOPC_SZ)
    val uopDBAR         = 59.U(UOPC_SZ)
    val uopIBAR         = 60.U(UOPC_SZ)

    val uopLD           = 61.U(UOPC_SZ)
    val uopSTA          = 62.U(UOPC_SZ)
    val uopSTD          = 63.U(UOPC_SZ)
}