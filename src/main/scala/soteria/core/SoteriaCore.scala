package soteria.core

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.rdd.RDD
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.{GCMParameterSpec, SecretKeySpec}
import java.security.SecureRandom
import scala.util.{Try, Success, Failure}

/**
 * SOTERIA Core Implementation
 * Based on the IEEE paper "Privacy-Preserving Machine Learning on Apache Spark"
 * Implements SGX-based privacy-preserving ML with computation partitioning
 * between SGX enclaves (sensitive operations) and untrusted environment (non-sensitive operations)
 */
object SoteriaCore {
  
  // Configuration constants
  private val ENCRYPTION_ALGORITHM = "AES"
  private val ENCRYPTION_TRANSFORMATION = "AES/GCM/NoPadding"
  private val GCM_IV_LENGTH = 12
  private val GCM_TAG_LENGTH = 16
  
  case class SoteriaConfig(
    enclaveEnabled: Boolean = true,
    encryptionEnabled: Boolean = true,
    partitioningStrategy: String = "COMPUTATION_PARTITIONING", // or "BASELINE"
    keySize: Int = 128,
    batchSize: Int = 1000
  )
  
  /**
   * Encrypted dataset wrapper
   */
  case class EncryptedDataset[T](
    data: Dataset[T],
    encryptionKey: SecretKey,
    isEncrypted: Boolean = true
  )
  
  /**
   * Computation partitioning manager
   * Decides which operations run inside SGX enclaves vs outside
   */
  object ComputationPartitioner {
    
    sealed trait ComputationZone
    case object EnclaveZone extends ComputationZone
    case object UntrustedZone extends ComputationZone
    
    /**
     * Determines computation zone based on operation sensitivity
     */
    def getComputationZone(operation: String): ComputationZone = operation match {
      case op if isSensitiveOperation(op) => EnclaveZone
      case _ => UntrustedZone
    }
    
    private def isSensitiveOperation(operation: String): Boolean = {
      val sensitiveOps = Set(
        "gradient_computation",
        "model_update", 
        "feature_extraction",
        "data_preprocessing",
        "model_inference"
      )
      sensitiveOps.contains(operation.toLowerCase)
    }
  }
  
  /**
   * AES-GCM encryption utilities
   */
  object EncryptionUtils {
    
    def generateKey(keySize: Int = 128): SecretKey = {
      val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM)
      keyGenerator.init(keySize)
      keyGenerator.generateKey()
    }
    
    def encrypt(data: Array[Byte], key: SecretKey): Try[Array[Byte]] = Try {
      val cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION)
      val iv = new Array[Byte](GCM_IV_LENGTH)
      new SecureRandom().nextBytes(iv)
      
      val gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
      cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
      
      val encryptedData = cipher.doFinal(data)
      iv ++ encryptedData
    }
    
    def decrypt(encryptedData: Array[Byte], key: SecretKey): Try[Array[Byte]] = Try {
      val iv = encryptedData.slice(0, GCM_IV_LENGTH)
      val cipherText = encryptedData.slice(GCM_IV_LENGTH, encryptedData.length)
      
      val cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION)
      val gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
      cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
      
      cipher.doFinal(cipherText)
    }
  }
  
  /**
   * SOTERIA Session - main entry point for privacy-preserving ML
   */
  class SoteriaSession(val spark: SparkSession, config: SoteriaConfig = SoteriaConfig()) {
    
    val masterKey = EncryptionUtils.generateKey(config.keySize)
    
    /**
     * Load and encrypt dataset
     */
    def loadEncryptedDataset[T](path: String)(implicit encoder: org.apache.spark.sql.Encoder[T]): EncryptedDataset[T] = {
      val rawData = spark.read.parquet(path).as[T]
      
      if (config.encryptionEnabled) {
        // In a real implementation, this would encrypt the actual data
        // For now, we'll mark it as encrypted and handle encryption at the RDD level
        EncryptedDataset(rawData, masterKey, isEncrypted = true)
      } else {
        EncryptedDataset(rawData, masterKey, isEncrypted = false)
      }
    }
    
    /**
     * Execute computation with partitioning strategy
     */
    def executeWithPartitioning[T, R](
      dataset: EncryptedDataset[T], 
      operation: String,
      computation: Dataset[T] => R
    ): R = {
      
      val computationZone = ComputationPartitioner.getComputationZone(operation)
      
      computationZone match {
        case ComputationPartitioner.EnclaveZone =>
          // Execute in SGX enclave (simulated)
          executeInEnclave(dataset, operation, computation)
          
        case ComputationPartitioner.UntrustedZone =>
          // Execute in untrusted environment
          executeInUntrusted(dataset, operation, computation)
      }
    }
    
    private def executeInEnclave[T, R](
      dataset: EncryptedDataset[T],
      operation: String, 
      computation: Dataset[T] => R
    ): R = {
      // Simulate enclave execution
      println(s"[SOTERIA-SGX] Executing $operation in SGX enclave...")
      
      // Decrypt data if needed (in real implementation)
      val processedData = if (dataset.isEncrypted) {
        // Decrypt within enclave
        dataset.data
      } else {
        dataset.data
      }
      
      // Execute computation
      val result = computation(processedData)
      
      println(s"[SOTERIA-SGX] Completed $operation in SGX enclave")
      result
    }
    
    private def executeInUntrusted[T, R](
      dataset: EncryptedDataset[T],
      operation: String,
      computation: Dataset[T] => R
    ): R = {
      println(s"[SOTERIA] Executing $operation in untrusted environment...")
      
      // Execute computation directly (data should remain encrypted if sensitive)
      val result = computation(dataset.data)
      
      println(s"[SOTERIA] Completed $operation in untrusted environment")
      result
    }
    
    def close(): Unit = {
      spark.close()
    }
  }
  
  /**
   * Factory method to create SOTERIA session
   */
  def createSession(appName: String, config: SoteriaConfig = SoteriaConfig()): SoteriaSession = {
    val spark = SparkSession.builder()
      .appName(appName)
      .config("spark.sql.adaptive.enabled", "true")
      .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
      .getOrCreate()
      
    new SoteriaSession(spark, config)
  }
}
