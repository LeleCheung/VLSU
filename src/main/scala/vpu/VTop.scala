package race.vpu

import chisel3._
import chisel3.util._
import VParams._

// TODO: new VTop or modify the old one?

class VTop extends Module {
  val io = IO(new Bundle {
    val dispatch_s2v = Flipped(DecoupledIO(new Dispatch_S2V))
    val tl = new TL // TileLink interface
    val debugRob = Option.when(debugMode)(Output(new FromCtrlToDebugRob))
  })

  val vCtrlBlock = Module(new VCtrlBlock)
  val vExuBlock = Module(new VExuBlock)
  val vLsuBlock = Module(new VLsuBlock)
  val vLsu = Module(new VLsu)

  vCtrlBlock.io.dispatch_s2v <> io.dispatch_s2v

  vExuBlock.io.in := vCtrlBlock.io.toExu
  vCtrlBlock.io.fromExu(0) := vExuBlock.io.out

  vCtrlBlock.io.lsu <> vLsuBlock.io.ctrl
  
  vLsuBlock.io.l2 <> vLsu.io.l2
  vLsu.io.tl <> io.tl

  if (debugMode) {
    io.debugRob.get := vCtrlBlock.io.debugRob.get
  }
}

object VerilogVTop extends App {
  println("Generating the VPU Top hardware")
  emitVerilog(new VTop(), Array("--target-dir", "build/verilog_vpu"))
}

