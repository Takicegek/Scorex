package scorex.block

import com.google.common.primitives.{Ints, Longs}
import io.circe.Json
import scorex.consensus.ConsensusModule
import scorex.crypto.hash.Sha256
import scorex.serialization.{BytesParseable, BytesSerializable, JsonSerializable}
import scorex.transaction.box.proposition.Proposition
import scorex.transaction.{Transaction, TransactionalModule}
import scorex.utils.ScorexLogging
import scala.util.Try
import io.circe.syntax._

/**
 * A block is an atomic piece of data network participates are agreed on.
 *
 * A block has:
 * - transactional data: a sequence of transactions, where a transaction is an atomic state update.
 * Some metadata is possible as well(transactions Merkle tree root, state Merkle tree root etc).
 *
 * - consensus data to check whether block was generated by a right party in a right way. E.g.
 * "baseTarget" & "generatorSignature" fields in the Nxt block structure, nonce & difficulty in the
 * Bitcoin block structure.
 *
 * - a signature(s) of a block generator(s)
 *
 * - additional data: block structure version no, timestamp etc
 */

class Block[P <: Proposition, TData <: TransactionalData[_ <: Transaction[P, _]], CData <: ConsensusData]
(val timestamp: Long,
 val consensusData: CData,
 val transactionalData: TData) extends BytesSerializable with JsonSerializable {

  type TDataExposed = TData
  type BlockId = ConsensusData.BlockId
  val version: Byte = (consensusData.version + transactionalData.version).toByte
  lazy val id: Array[Byte] = consensusData.id

  /**
   * Whether a block contains transactions, or a header only
   *
   * @return true if the block contains header just
   */
  lazy val headerOnly: Boolean = transactionalData.headerOnly

  /**
   * Whether a block contains transactions, or a header only
   *
   * @return true if the block contains transactions, so a full block
   */
  def fullBlock: Boolean = !headerOnly

  lazy val bytes: Array[Byte] = (version +: Longs.toByteArray(timestamp)) ++ arrayWithSize(consensusData.bytes) ++
    arrayWithSize(transactionalData.bytes)

  lazy val json: Json = Map(
    "version" -> version.toString.asJson,
    "timestamp" -> timestamp.asJson,
    "consensusData" -> consensusData.json,
    "transactionalData" -> transactionalData.json
  ).asJson
}

object Block extends ScorexLogging {

  def parseBytes[P <: Proposition, TX <: Transaction[P, TX], TData <: TransactionalData[TX], CData <: ConsensusData]
  (bytes: Array[Byte])
  (implicit consensusParser: BytesParseable[CData],
   transactionalParser: BytesParseable[TData]): Try[Block[P, TData, CData]] = Try {
    val version = bytes.head
    val timestamp = Longs.fromByteArray(bytes.slice(1, 9))
    val cDataSize = Ints.fromByteArray(bytes.slice(9, 13))
    val cData = consensusParser.parseBytes(bytes.slice(13, 13 + cDataSize)).get
    val tDataSize = Ints.fromByteArray(bytes.slice(13 + cDataSize, 17 + cDataSize))
    val tData = transactionalParser.parseBytes(bytes.slice(17 + cDataSize, 17 + cDataSize + tDataSize)).get
    require(version == (cData.version + tData.version).toByte)
    new Block[P, TData, CData](timestamp, cData, tData)
  }

  def build[P <: Proposition, TX <: Transaction[P, TX], CData <: ConsensusData, TData <: TransactionalData[TX]]
  (consensusData: CData)
  (transactionalData: TData): Block[P, TData, CData] = {

    val timestamp = System.currentTimeMillis()
    new Block(timestamp, consensusData, transactionalData)
  }

  def genesis[P <: Proposition, TX <: Transaction[P, TX], TData <: TransactionalData[TX], CData <: ConsensusData]
  (genesisTimestamp: Long)
  (implicit consensusModule: ConsensusModule[P, CData],
   transactionalModule: TransactionalModule[P, TX, TData]): Block[P, TData, CData] = {

    new Block(genesisTimestamp, consensusModule.genesisData, transactionalModule.genesisData)
  }

  /*
  def isValid[P <: Proposition, TX <: Transaction[P, TX], TData <: TransactionalData[TX], CData <: ConsensusData]
  (block: Block[P, TData, CData])
  (implicit consensusModule: ConsensusModule[P, TX, TData, CData],
   transactionalModule: TransactionalModule[P, TX, TData]): Boolean = {

    if (consensusModule.contains(block)) true //applied blocks are valid
    else {
      lazy val consensus = consensusModule.isValid(block)
      lazy val transaction = transactionalModule.isValid(block.transactionalData)

      if (!consensus) log.debug(s"Invalid consensus data in block ${consensusModule.encodedId(block)}")
      else if (!transaction) log.debug(s"Invalid transaction data in block ${consensusModule.encodedId(block)}")

      consensus && transaction
    }
  }*/
}