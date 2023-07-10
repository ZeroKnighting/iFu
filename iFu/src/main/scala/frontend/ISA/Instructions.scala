package frontend.ISA

import chisel3.util.BitPat

object Instructions {
    def ADDW            = BitPat("b00000000000100000???????????????")
    def SUBW            = BitPat("b00000000000100010???????????????")
    def SLT             = BitPat("b00000000000100100???????????????")
    def SLTU            = BitPat("b00000000000100101???????????????")
    def NOR             = BitPat("b00000000000101000???????????????")
    def AND             = BitPat("b00000000000101001???????????????")
    def OR              = BitPat("b00000000000101010???????????????")
    def XOR             = BitPat("b00000000000101011???????????????")
    def LU12IW          = BitPat("b0001010?????????????????????????")
    def ADDIW           = BitPat("b0000001010??????????????????????")
    def SLTI            = BitPat("b0000001000??????????????????????")
    def SLTUI           = BitPat("b0000001001??????????????????????")
    def PCADDI          = BitPat("b0001100?????????????????????????")
    def PCADDU12I       = BitPat("b0001110?????????????????????????")
    def ANDN            = BitPat("b00000000000101101???????????????")
    def ORN             = BitPat("b00000000000101100???????????????")
    def ANDI            = BitPat("b0000001101??????????????????????")
    def ORI             = BitPat("b0000001110??????????????????????")
    def XORI            = BitPat("b0000001111??????????????????????")
    def MULW            = BitPat("b00000000000111000???????????????")
    def MULHW           = BitPat("b00000000000111001???????????????")
    def MULHWU          = BitPat("b00000000000111010???????????????")
    def DIVW            = BitPat("b00000000001000000???????????????")
    def MODW            = BitPat("b00000000001000001???????????????")
    def DIVWU           = BitPat("b00000000001000010???????????????")
    def MODWU           = BitPat("b00000000001000011???????????????")
    def SLLIW           = BitPat("b00000000010000001???????????????")
    def SRLIW           = BitPat("b00000000010001001???????????????")
    def SRAIW           = BitPat("b00000000010010001???????????????")
    def SLLW            = BitPat("b00000000000101110???????????????")
    def SRLW            = BitPat("b00000000000101111???????????????")
    def SRAW            = BitPat("b00000000000110000???????????????")
    def JIRL            = BitPat("b010011??????????????????????????")
    def B               = BitPat("b010100??????????????????????????")
    def BL              = BitPat("b010101??????????????????????????")
    def BEQ             = BitPat("b010110??????????????????????????")
    def BNE             = BitPat("b010111??????????????????????????")
    def BLT             = BitPat("b011000??????????????????????????")
    def BGE             = BitPat("b011001??????????????????????????")
    def BLTU            = BitPat("b011010??????????????????????????")
    def BGEU            = BitPat("b011011??????????????????????????")
    def LLW             = BitPat("b00100000????????????????????????")
    def SCW             = BitPat("b00100001????????????????????????")
    def LDB             = BitPat("b0010100000??????????????????????")
    def LDBU            = BitPat("b0010101000??????????????????????") 
    def LDH             = BitPat("b0010100001??????????????")
    def LDHU            = BitPat("b0010101001??????????????")
    def LDW             = BitPat("b0010100010??????????????")
    def STB             = BitPat("b0010100100??????????????")
    def STH             = BitPat("b0010100101??????????????")
    def STW             = BitPat("b0010100110??????????????")
    def SYSCALL         = BitPat("b00000000001010110???????????????")
    def BREAK           = BitPat("b00000000001010100???????????????")
    def CSRRD           = BitPat("b00000100??????????????00000?????")
    def CSRWR           = BitPat("b00000100??????????????00001?????")
    def CSRXCHG         = BitPat("b00000100?????????????1?????????")|BitPat("b00000100???????????????1????????")|("b00000100????????????????1???????")|("b00000100?????????????????1??????") //rj!=0或1
    def ERTN            = BitPat("b00000110010010000011100000000000")
    def IDLE            = BitPat("b00000110010010001???????????????")
    def TLBSRCH         = BitPat("b00000110010010000010100000000000")
    def TLBRD           = BitPat("b00000110010010000010110000000000")
    def TLBWR           = BitPat("b00000110010010000011000000000000")
    def TLBFILL         = BitPat("b00000110010010000011010000000000")
    def INVTLB          = BitPat("b00000110010010011???????????????")
    def CACOP           = BitPat("b0000011000??????????????????????")
    def PRELD           = BitPat("b0010101011??????????????????????")
    def DBAR            = BitPat("b00111000011100100???????????????")
    def IBAR            = BitPat("b00111000011100101???????????????")

}
