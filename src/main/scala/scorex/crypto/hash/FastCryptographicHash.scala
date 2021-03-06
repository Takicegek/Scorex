package scorex.crypto.hash

/**
 * Interface for fast and secure Blake2b hash function
 */

object FastCryptographicHash extends CryptographicHash32 {

  override def hash(input: Message): Digest = Blake2b256.hash(input)

}
