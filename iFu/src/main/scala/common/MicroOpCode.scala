package iFu.common

import chisel3._
import chisel3.util._

trait MicroOpCode {
    val UOPC_SZ = 6
    val uopX            = BitPat.dontCare(UOPC_SZ)

    val uopNOP          = 0.U(UOPC_SZ.W)
    val uopADD          = 1.U(UOPC_SZ.W)
    val uopSUB          = 2.U(UOPC_SZ.W)
    val uopSLT          = 3.U(UOPC_SZ.W)
    val uopSLTU         = 4.U(UOPC_SZ.W)
    val uopNOR          = 5.U(UOPC_SZ.W)
    val uopAND          = 6.U(UOPC_SZ.W)
    val uopOR           = 7.U(UOPC_SZ.W)
    val uopXOR          = 8.U(UOPC_SZ.W)
    val uopLU12IW       = 9.U(UOPC_SZ.W)
    val uopADDIW        = 10.U(UOPC_SZ.W)
    val uopSLTI         = 11.U(UOPC_SZ.W)
    val uopSLTUI        = 12.U(UOPC_SZ.W)
    val uopPCADDI       = 13.U(UOPC_SZ.W)
    val uopPCADDU12I    = 14.U(UOPC_SZ.W)
    val uopANDN         = 15.U(UOPC_SZ.W) // TODO: should alu do this?
    val uopORN          = 16.U(UOPC_SZ.W) // TODO: should alu do this?
    val uopANDI         = 17.U(UOPC_SZ.W)
    val uopORI          = 18.U(UOPC_SZ.W)
    val uopXORI         = 19.U(UOPC_SZ.W)
    val uopMULW         = 20.U(UOPC_SZ.W)
    val uopMULHW        = 21.U(UOPC_SZ.W)
    val uopMULHWU       = 22.U(UOPC_SZ.W)
    val uopDIVW         = 23.U(UOPC_SZ.W)
    val uopMODW         = 24.U(UOPC_SZ.W)
    val uopDIVWU        = 25.U(UOPC_SZ.W)
    val uopMODWU        = 26.U(UOPC_SZ.W)
    val uopSLLIW        = 27.U(UOPC_SZ.W)
    val uopSRLIW        = 28.U(UOPC_SZ.W)
    val uopSRAIW        = 29.U(UOPC_SZ.W)
    val uopSLLW         = 30.U(UOPC_SZ.W)
    val uopSRLW         = 31.U(UOPC_SZ.W)
    val uopSRAW         = 32.U(UOPC_SZ.W)
    val uopJIRL         = 33.U(UOPC_SZ.W)
    val uopB            = 35.U(UOPC_SZ.W)
    val uopBL           = 36.U(UOPC_SZ.W)
    val uopBEQ          = 37.U(UOPC_SZ.W)
    val uopBNE          = 38.U(UOPC_SZ.W)
    val uopBLT          = 39.U(UOPC_SZ.W)
    val uopBGE          = 40.U(UOPC_SZ.W)
    val uopBLTU         = 41.U(UOPC_SZ.W)
    val uopBGEU         = 42.U(UOPC_SZ.W)

    val uopERET         = 43.U(UOPC_SZ.W)
    val uopCSRRD        = 44.U(UOPC_SZ.W)
    val uopCSRWR        = 45.U(UOPC_SZ.W)
    val uopCSRXCHG      = 46.U(UOPC_SZ.W)

    val uopIDLE         = 47.U(UOPC_SZ.W)

    val uopTLBSRCH      = 48.U(UOPC_SZ.W)
    val uopTLBRD        = 49.U(UOPC_SZ.W)
    val uopTLBWR        = 50.U(UOPC_SZ.W)
    val uopTLBFILL      = 51.U(UOPC_SZ.W)
    val uopINVTLB       = 52.U(UOPC_SZ.W)
    
    val uopCACOP        = 53.U(UOPC_SZ.W)
    val uopPRELD        = 54.U(UOPC_SZ.W)
    val uopDBAR         = 55.U(UOPC_SZ.W)
    val uopIBAR         = 56.U(UOPC_SZ.W)

    val uopLD           = 57.U(UOPC_SZ.W)
    val uopSTA          = 58.U(UOPC_SZ.W)
    val uopSTD          = 59.U(UOPC_SZ.W)

    val uopRDCNTIDW     = 60.U(UOPC_SZ.W)
    val uopRDCNTVLW     = 61.U(UOPC_SZ.W)
    val uopRDCNTVHW     = 62.U(UOPC_SZ.W)

    val uopMove         = 63.U(UOPC_SZ.W)
}