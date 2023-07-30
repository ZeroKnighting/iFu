package iFu.backend

import chisel3._
import chisel3.util._

import iFu.common._
import iFu.common.Consts._

import scala.collection.mutable.ArrayBuffer

class ExecutionUnits extends CoreModule {
    val totalIssueWidth = issueParams.map(_.issueWidth).sum

    private val exe_units = ArrayBuffer[ExecutionUnit]()

    def length = exe_units.length

    def apply(n: Int) = exe_units(n)

    def map[T](f: ExecutionUnit => T) = {
        exe_units.map(f)
    }

    def withFilter(f: ExecutionUnit => Boolean) = {
        exe_units.withFilter(f)
    }

    def foreach[U](f: ExecutionUnit => U) = {
        exe_units.foreach(f)
    }

    def zipWithIndex = {
        exe_units.zipWithIndex
    }

    def indexWhere(f: ExecutionUnit => Boolean) = {
        exe_units.indexWhere(f)
    }

    def count(f: ExecutionUnit => Boolean) = {
        exe_units.count(f)
    }

    lazy val memory_units = {
        exe_units.filter(_.hasMem)
    }

    lazy val alu_units = {
        exe_units.filter(_.hasAlu)
    }

    // lazy val csr_unit = {
    //     require(exe_units.count(_.hasCSR) == 1)
    //     exe_units.find(_.hasCSR).get
    // }

    lazy val jmp_unit_idx = {
        exe_units.indexWhere(_.hasJmpUnit)
    }

    val int_width = issueParams.find(_.iqType == IQT_INT.litValue).get.issueWidth
    val mem_width = issueParams.find(_.iqType == IQT_MEM.litValue).get.issueWidth

    for (w <- 0 until mem_width) {
        val memExeUnit = Module(new ALUExeUnit(
            hasAlu = false,
            hasMem = true
        ))
        memExeUnit.io.mem_iresp.ready := DontCare
        exe_units += memExeUnit
    }
    for (w <- 0 until int_width) {
        def is_nth(n: Int): Boolean = w == ((n) % int_width)

        val alu_exe_unit = Module(new ALUExeUnit(
            hasJmpUnit = is_nth(0),
            // hasCSR = is_nth(1),
            hasMul = is_nth(2),
            hasDiv = is_nth(3),
        ))
        exe_units += alu_exe_unit
    }

    val numIrfReaders       = exe_units.count(_.readsIrf)
    val numIrfReadPorts     = exe_units.count(_.readsIrf) * 2
    val numIrfWritePorts    = exe_units.count(_.writesIrf)
    val numMemIrfWritePorts = exe_units.count(_.writesMemIrf)
    val numTotalBypassPorts = exe_units.withFilter(_.bypassable).map(_.numStages).foldLeft(0)(_ + _)

    val bypassable_write_port_mask = exe_units.withFilter(x => x.writesIrf).map(u => u.bypassable)
}
