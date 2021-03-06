package scorex.network.message

import java.net.{InetAddress, InetSocketAddress}
import java.util

import com.google.common.primitives.{Bytes, Ints}
import scorex.block.ConsensusData.BlockId
import scorex.block.{Block, ConsensusData, TransactionalData}
import scorex.consensus.ConsensusModule
import scorex.crypto.signatures.SigningFunctions
import scorex.network.message.Message._
import scorex.serialization.BytesParseable
import scorex.transaction.{Transaction, TransactionalModule}
import scorex.transaction.box.proposition.Proposition
import scorex.transaction.proof.Signature25519

import scala.util.{Success, Try}


trait SignaturesSeqSpec extends MessageSpec[Seq[SigningFunctions.Signature]] {

  private val SignatureLength = Signature25519.SignatureSize
  private val DataLength = 4

  override def deserializeData(bytes: Array[Byte]): Try[Seq[SigningFunctions.Signature]] = Try {
    val lengthBytes = bytes.take(DataLength)
    val length = Ints.fromByteArray(lengthBytes)

    assert(bytes.length == DataLength + (length * SignatureLength), "Data does not match length")

    (0 until length).map { i =>
      val position = DataLength + (i * SignatureLength)
      bytes.slice(position, position + SignatureLength)
    }
  }

  override def serializeData(signatures: Seq[SigningFunctions.Signature]): Array[Byte] = {
    val length = signatures.size
    val lengthBytes = Ints.toByteArray(length)

    //WRITE SIGNATURES
    signatures.foldLeft(lengthBytes) { case (bs, header) => Bytes.concat(bs, header) }
  }
}

object GetSignaturesSpec extends SignaturesSeqSpec {
  override val messageCode: MessageCode = 20: Byte
  override val messageName: String = "GetSignatures message"
}

object SignaturesSpec extends SignaturesSeqSpec {
  override val messageCode: MessageCode = 21: Byte
  override val messageName: String = "Signatures message"
}

object GetBlockSpec extends MessageSpec[BlockId] {
  override val messageCode: MessageCode = 22: Byte
  override val messageName: String = "GetBlock message"

  override def serializeData(id: BlockId): Array[Byte] = id

  override def deserializeData(bytes: Array[Byte]): Try[BlockId] = Success(bytes)

}

class BlockMessageSpec[P <: Proposition, TX <: Transaction[P, TX], TD <: TransactionalData[TX], CD <: ConsensusData]
(consensusParser: BytesParseable[CD],
 transactionalParser: BytesParseable[TD]) extends MessageSpec[Block[P, TD, CD]] {

  override val messageCode: MessageCode = 23: Byte

  override val messageName: String = "Block message"

  override def serializeData(block: Block[P, TD, CD]): Array[Byte] = block.bytes

  override def deserializeData(bytes: Array[Byte]): Try[Block[P, TD, CD]] =
    Block.parseBytes[P, TX, TD, CD](bytes)(consensusParser, transactionalParser)
}

object ScoreMessageSpec extends MessageSpec[BigInt] {
  override val messageCode: MessageCode = 24: Byte

  override val messageName: String = "Score message"

  override def serializeData(score: BigInt): Array[Byte] = {
    val scoreBytes = score.toByteArray
    val bb = java.nio.ByteBuffer.allocate(scoreBytes.length)
    bb.put(scoreBytes)
    bb.array()
  }

  override def deserializeData(bytes: Array[Byte]): Try[BigInt] = Try {
    BigInt(1, bytes)
  }
}


object GetPeersSpec extends MessageSpec[Unit] {
  override val messageCode: Message.MessageCode = 1: Byte

  override val messageName: String = "GetPeers message"

  override def deserializeData(bytes: Array[Byte]): Try[Unit] =
    Try(require(bytes.isEmpty, "Non-empty data for GetPeers"))

  override def serializeData(data: Unit): Array[Byte] = Array()
}

object PeersSpec extends MessageSpec[Seq[InetSocketAddress]] {
  private val AddressLength = 4
  private val PortLength = 4
  private val DataLength = 4

  override val messageCode: Message.MessageCode = 2: Byte

  override val messageName: String = "Peers message"

  override def deserializeData(bytes: Array[Byte]): Try[Seq[InetSocketAddress]] = Try {
    val lengthBytes = util.Arrays.copyOfRange(bytes, 0, DataLength)
    val length = Ints.fromByteArray(lengthBytes)

    assert(bytes.length == DataLength + (length * (AddressLength + PortLength)), "Data does not match length")

    (0 until length).map { i =>
      val position = lengthBytes.length + (i * (AddressLength + PortLength))
      val addressBytes = util.Arrays.copyOfRange(bytes, position, position + AddressLength)
      val address = InetAddress.getByAddress(addressBytes)
      val portBytes = util.Arrays.copyOfRange(bytes, position + AddressLength, position + AddressLength + PortLength)
      new InetSocketAddress(address, Ints.fromByteArray(portBytes))
    }
  }

  override def serializeData(peers: Seq[InetSocketAddress]): Array[Byte] = {
    val length = peers.size
    val lengthBytes = Ints.toByteArray(length)

    peers.foldLeft(lengthBytes) { case (bs, peer) =>
      Bytes.concat(bs, peer.getAddress.getAddress, Ints.toByteArray(peer.getPort))
    }
  }
}
