package scorex.transaction.wallet

import java.io.File

import com.google.common.primitives.{Bytes, Ints}
import org.h2.mvstore.{MVMap, MVStore}
import scorex.crypto.encode.Base58
import scorex.crypto.hash.SecureCryptographicHash
import scorex.settings.Settings
import scorex.transaction.box.Box
import scorex.transaction.box.proposition.Proposition
import scorex.transaction.state.Secret
import scorex.utils.{ScorexLogging, randomBytes}

import scala.collection.JavaConversions._
import scala.collection.concurrent.TrieMap

case class WalletChanges[P <: Proposition](toRemove: Set[Box[P]], toAppend: Set[Box[P]])

//todo: add txs watching
//todo: implement/fix missed
class Wallet[P <: Proposition, S <: Secret](settings: Settings)
  extends ScorexLogging {

  private val NonceFieldName = "nonce"

  val walletFileOpt: Option[File] = settings.walletDirOpt.map(walletDir => new java.io.File(walletDir, "wallet.s.dat"))
  val password: String = settings.walletPassword
  val seedOpt: Option[Array[Byte]] = settings.walletSeed

  private val database: MVStore = walletFileOpt match {
    case Some(walletFile) =>
      //create parent folders then check their existence
      walletFile.getParentFile.mkdirs().ensuring(walletFile.getParentFile.exists())
      new MVStore.Builder().fileName(walletFile.getAbsolutePath).encryptionKey(password.toCharArray).compress().open()

    case None => new MVStore.Builder().open()
  }

  private val secretsPersistence: MVMap[Int, Array[Byte]] = database.openMap("secretHolders")
  private val seedPersistence: MVMap[String, Array[Byte]] = database.openMap("seed")
  private val noncePersistence: MVMap[String, Int] = database.openMap("nonce")

  if (Option(seedPersistence.get("seed")).isEmpty) {
    val seed = seedOpt.getOrElse {
      val Attempts = 10
      val SeedSize = 64
      lazy val randomSeed = randomBytes(SeedSize)
      def readSeed(limit: Int = Attempts): Array[Byte] = {
        println("Please type your wallet seed or type Enter to generate random one")
        val typed = scala.io.StdIn.readLine()
        if (typed.isEmpty) {
          println(s"Your random generated seed is ${Base58.encode(randomSeed)}")
          randomSeed
        } else
          Base58.decode(typed).getOrElse {
            if (limit > 0) {
              println("Wallet seed should be correct Base58 encoded string.")
              readSeed(limit - 1)
            } else throw new Error("Sorry you have made too many incorrect seed guesses")
          }
      }
      readSeed()
    }
    seedPersistence.put("seed", seed)
  }

  private lazy val seed: Array[Byte] = seedPersistence.get("seed")

  private val secretsCache: TrieMap[String, S] = ??? /*{
    val shs = secretsPersistence
      .keys
      .map(k => secretsPersistence.get(k))
      .map(seed => generator.generateKeys(seed))

    TrieMap(shs.map(sh => sh.publicAddress -> sh).toSeq: _*)
  }*/

  def privateKeyAccounts(): Seq[S] = secretsCache.values.toSeq

  def privateKeyAccount(): S = secretsCache.values.headOption.getOrElse(generateNewAccount())

  def generateNewAccounts(howMany: Int): Seq[S] = (1 to howMany).map(_ => generateNewAccount())

  def generateNewAccount(): S = synchronized {
    ???
    /*val nonce = getAndIncrementNonce()

    val accountSeed = generateSecretSeed(seed, nonce)
    val secretHolder: S = generator.generateKeys(accountSeed)

    secretsCache += secretHolder.publicAddress -> secretHolder
    secretsPersistence.put(secretsPersistence.lastKey() + 1, secretHolder.bytes)
    database.commit()

    log.info("Added account #" + secretsCache.size)
    secretHolder*/
  }

  def generateSecretSeed(seed: Array[Byte], nonce: Int): Array[Byte] =
    SecureCryptographicHash(Bytes.concat(Ints.toByteArray(nonce), seed))

  def deleteSecret(secret: S): Boolean = synchronized {
    ???
    /*
    val res = secretsPersistence.keys.find { k =>
      if (secretsPersistence.get(k) sameElements secret.bytes) {
        secretsPersistence.remove(k)
        true
      } else false
    }
    database.commit()
    secretsCache -= secret.publicAddress
    res.isDefined*/
  }

  def correspondingSecret(publicAddress: String): Option[S] = secretsCache.get(publicAddress)

  def close(): Unit = if (!database.isClosed) {
    database.commit()
    database.close()
    secretsCache.clear()
  }

  def exists(): Boolean = walletFileOpt.forall(_.exists())

  def nonce(): Int = Option(noncePersistence.get(NonceFieldName)).getOrElse(0)

  def getAndIncrementNonce(): Int = synchronized {
    noncePersistence.put(NonceFieldName, nonce() + 1)
  }

  //  def scanChanges[TX <: Transaction[P, TX]](transactions: Iterable[TX]): WalletChanges[P] = {
  //    val (r, a) = transactions.foldLeft(Set[Box[P]]() -> Set[Box[P]]()) { case ((btr, bta), tx) =>
  //      (btr ++ tx.boxesToRemove.toSet, bta ++ tx.boxesToAdd.toSet)
  //    }
  //    WalletChanges(r, a)
  //  }
}