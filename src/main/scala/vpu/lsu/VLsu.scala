package race.vpu

import chisel3._
import chisel3.util._
import VParams._

/* 
import freechips.rocketchip.tilelink._
import coupledL2.{EnableCHI, L2ParamKey,MatrixDataBundle,MatrixKey}

import coupledL2.tl2tl.TL2TLCoupledL2
import coupledL2.tl2chi.{CHIIssue, PortIO, TL2CHICoupledL2}
import huancun.BankBitsKey
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink.TLPermissions._
import coupledL2._
*/


// VLsu: transform the theoretical l2 interface to the tilelink interface
class VLsu extends Module {
  val io = IO(new Bundle {
    val l2 = new Bundle {
      val loadReq  = Flipped(DecoupledIO(new VL2LoadReq))
      val loadRsp  = Flipped(Input(ValidIO(new VL2LoadRsp)))
      val storeReq = Flipped(DecoupledIO(new VL2StoreReq))
      val storeAck = Flipped(Input(ValidIO(new VL2StoreAck)))
    }
    val tl = new TL // TileLink interface
  })



  for (i <- 0 until 8) {
    // Default values for A-channel
    io.tl.tlink(i).a.valid := false.B
    io.tl.tlink(i).a.bits.a_opcode := 0.U // Or a suitable idle opcode if TL has one
    io.tl.tlink(i).a.bits.a_param := 0.U
    io.tl.tlink(i).a.bits.a_size := 0.U
    io.tl.tlink(i).a.bits.a_source := 0.U
    io.tl.tlink(i).a.bits.a_address := 0.U
    io.tl.tlink(i).a.bits.a_user_matrix := 0.U
    io.tl.tlink(i).a.bits.a_mask := 0.U
    io.tl.tlink(i).a.bits.a_data := 0.U
    io.tl.tlink(i).a.bits.a_corrupt := false.B // Use false.B for boolean

    // Set D-channel ready (receiver side)
    io.tl.tlink(i).d.ready := false.B // Assuming D channel is not used for load/store responses directly in this simplified snippet
    // M-channel ready for responses (receiver side)
    io.tl.tlink(i).m.ready := true.B // Always ready to receive responses


  }

  for (i <- 0 until nPortsL2) {
    io.l2.storeAck.bits.dummy := false.B // Dummy response for store acknowledgment
    io.l2.storeAck.valid := false.B // Store acknowledgment is not valid by default
    io.l2.loadReq.ready := false.B // Load request is not ready by default
    io.l2.storeReq.ready := false.B // Store request is not ready by default
    // If loadReq is valid, then drive the A-channel with load request details
    when (io.l2.loadReq.valid) {
      io.tl.tlink(i).a.valid          := true.B // Set valid for the active request
      io.tl.tlink(i).a.bits.a_opcode  := 4.U // TLMessages.Get
      io.tl.tlink(i).a.bits.a_param   := 0.U
      io.tl.tlink(i).a.bits.a_size    := 6.U // 64B
      io.tl.tlink(i).a.bits.a_source  := i.U // TODO: Ensure unique source IDs if multiple requests can be outstanding
      io.tl.tlink(i).a.bits.a_address := io.l2.loadReq.bits.addr(i)
      io.tl.tlink(i).a.bits.a_user_matrix := 0.U // vec, not matrix
      io.tl.tlink(i).a.bits.a_mask    := 0.U
      io.tl.tlink(i).a.bits.a_data    := 0.U // Only used for store
      io.tl.tlink(i).a.bits.a_corrupt := false.B
    }
    io.tl.tlink(i).m.ready              := true.B // receiver side
    io.l2.loadRsp.valid                 := io.tl.tlink(i).m.valid
    io.l2.loadRsp.bits.data(i)          := io.tl.tlink(i).m.bits.m_data // Load data
  }

  // TODO: support consecutive load requests, so we need to buffer (source, data) pairs
  /*
  // response bundle carrying the TL source field and the returned data
  class RespBuf extends AMUBundle {
    val source = UInt(5.W)
    val data   = UInt(mGetBits.W)
  }

  // round-robin arbiter to pick at most one incoming M-channel beat per cycle
  val mArb = Module(new RRArbiter(new RespBuf, nPortsL2))
  for (i <- 0 until nPortsL2) {
    val mCh = io.tl.tlink(i).m
    mArb.io.in(i).valid      := mCh.valid
    mArb.io.in(i).bits.source:= mCh.bits.m_source
    mArb.io.in(i).bits.data  := mCh.bits.m_data
    mCh.ready                := mArb.io.in(i).ready
  }

  // queue to buffer outstanding (source,data) pairs
  val respQ = Module(new Queue(new RespBuf, 16))
  respQ.io.enq <> mArb.io.out

  // drive the l2.loadRsp from the head of the queue
  // respQ.io.deq.ready        := io.l2.loadRsp.ready
  io.l2.loadRsp.valid       := respQ.io.deq.valid
  // zero the Vec before writing the one lane we care about
  io.l2.loadRsp.bits.data   := 0.U.asTypeOf(io.l2.loadRsp.bits.data)
  when (respQ.io.deq.fire) {
    val r = respQ.io.deq.bits
    io.l2.loadRsp.bits.data(r.source) := r.data
  }
  */

    
}


