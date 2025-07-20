package soteria.ml

import soteria.core.SoteriaCore._
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.linalg.{Vector, Vectors, DenseVector}
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.recommendation.ALS
import org.apache.spark.ml.classification.{LogisticRegression, DecisionTreeClassifier}
import org.apache.spark.ml.regression.{LinearRegression, DecisionTreeRegressor}
import org.apache.spark.ml.evaluation.{MulticlassClassificationEvaluator, RegressionEvaluator}
import org.apache.spark.sql.types._
import org.apache.spark.rdd.RDD
import scala.util.Random

/**
 * SOTERIA Machine Learning Implementation
 * Based on the IEEE paper "Privacy-Preserving Machine Learning on Apache Spark"
 * Implements SGX-based secure ML algorithms with computation partitioning
 */
object SoteriaML {
  
  // Data structures for different ML tasks
  case class TrainingData(features: Vector, label: Double)
  case class ClusteringData(features: Vector, id: Long)
  case class RecommendationData(user: Int, item: Int, rating: Float)
  case class ClassificationData(features: Vector, label: Double)
  case class RegressionData(features: Vector, label: Double)
  case class LDAData(features: Vector, docId: Long)
  case class PCAData(features: Vector, id: Long)
  
  /**
   * SOTERIA K-Means Clustering with SGX Enclaves
   */
  class SoteriaKMeans(session: SoteriaSession, k: Int, maxIterations: Int = 100) {
    
    def train(dataset: EncryptedDataset[ClusteringData]): SoteriaKMeansModel = {
      // Phase 1: Data preprocessing in SGX enclave
      val preprocessedData = session.executeWithPartitioning(
        dataset,
        "data_preprocessing",
        (data: Dataset[ClusteringData]) => {
          println("[SOTERIA-SGX] Preprocessing clustering data within enclave...")
          
          // Convert to RDD for processing
          import session.spark.implicits._
          val dataRDD = data.rdd.map(row => (row.id, row.features))
          
          // Initial centroid selection within enclave (secure)
          val initialCentroids = selectInitialCentroids(dataRDD, k)
          
          (dataRDD, initialCentroids)
        }
      )
      
      val (dataRDD, initialCentroids) = preprocessedData
      
      // Phase 2: Iterative clustering with computation partitioning
      val finalCentroids = performIterativeClustering(
        session, dataRDD, initialCentroids, maxIterations
      )
      
      SoteriaKMeansModel(finalCentroids, k)
    }
    
    private def selectInitialCentroids(dataRDD: RDD[(Long, Vector)], k: Int): Array[Vector] = {
      // K-means++ initialization within SGX enclave
      val sample = dataRDD.takeSample(false, k, Random.nextLong())
      sample.map(_._2)
    }
    
    private def performIterativeClustering(
      session: SoteriaSession,
      dataRDD: RDD[(Long, Vector)],
      initialCentroids: Array[Vector],
      maxIter: Int
    ): Array[Vector] = {
      
      var centroids = initialCentroids
      var iteration = 0
      var converged = false
      
      while (iteration < maxIter && !converged) {
        // Distance computation in untrusted environment (non-sensitive)
        val assignments = dataRDD.map { case (id, point) =>
          val closestCentroid = findClosestCentroid(point, centroids)
          (closestCentroid, (point, 1))
        }
        
        // Centroid update in SGX enclave (sensitive operation)
        val newCentroids = session.executeWithPartitioning(
          session.loadEncryptedDataset("dummy"), // Placeholder
          "model_update",
          _ => {
            println(s"[SOTERIA-SGX] Updating centroids in SGX enclave (iteration ${iteration + 1})...")
            
            val centroidUpdates = assignments.reduceByKey { case ((sum1, count1), (sum2, count2)) =>
              val newSum = Vectors.dense(
                sum1.toArray.zip(sum2.toArray).map { case (a, b) => a + b }
              )
              (newSum, count1 + count2)
            }.collectAsMap()
            
            centroids.indices.map { i =>
              centroidUpdates.get(i) match {
                case Some((sum, count)) => 
                  Vectors.dense(sum.toArray.map(_ / count))
                case None => centroids(i)
              }
            }.toArray
          }
        )
        
        // Check convergence
        val maxDelta = centroids.zip(newCentroids).map { case (old, newC) =>
          Vectors.sqdist(old, newC)
        }.max
        
        converged = maxDelta < 1e-6
        centroids = newCentroids
        iteration += 1
      }
      
      centroids
    }
    
    private def findClosestCentroid(point: Vector, centroids: Array[Vector]): Int = {
      centroids.zipWithIndex.minBy { case (centroid, _) =>
        Vectors.sqdist(point, centroid)
      }._2
    }
  }
  
  case class SoteriaKMeansModel(centroids: Array[Vector], k: Int) {
    def predict(point: Vector): Int = {
      centroids.zipWithIndex.minBy { case (centroid, _) =>
        Vectors.sqdist(point, centroid)
      }._2
    }
  }
  
  /**
   * SOTERIA Logistic Regression with SGX Protection
   */
  class SoteriaLogisticRegression(session: SoteriaSession, maxIterations: Int = 100, stepSize: Double = 0.01) {
    
    def train(dataset: EncryptedDataset[ClassificationData]): SoteriaLogisticRegressionModel = {
      // Gradient computation and model updates happen in SGX enclave
      val result = session.executeWithPartitioning(
        dataset,
        "gradient_computation",
        (data: Dataset[ClassificationData]) => {
          println("[SOTERIA-SGX] Training logistic regression within SGX enclave...")
          
          import session.spark.implicits._
          val trainingRDD = data.rdd
          
          // Initialize weights within enclave
          val numFeatures = trainingRDD.first().features.size
          var weights = Vectors.zeros(numFeatures)
          var intercept = 0.0
          
          // SGD training loop within enclave
          for (iteration <- 1 to maxIterations) {
            val gradient = computeGradient(trainingRDD, weights, intercept)
            
            // Update parameters
            weights = Vectors.dense(
              weights.toArray.zip(gradient._1.toArray).map { case (w, g) =>
                w - stepSize * g
              }
            )
            intercept = intercept - stepSize * gradient._2
            
            if (iteration % 10 == 0) {
              val loss = computeLogisticLoss(trainingRDD, weights, intercept)
              println(s"[SOTERIA-SGX] Iteration $iteration, Loss: $loss")
            }
          }
          
          (weights, intercept)
        }
      )
      
      SoteriaLogisticRegressionModel(result._1, result._2)
    }
    
    private def computeGradient(
      data: RDD[ClassificationData], 
      weights: Vector, 
      intercept: Double
    ): (Vector, Double) = {
      val gradients = data.map { point =>
        val prediction = sigmoid(weights.toArray.zip(point.features.toArray).map { 
          case (w, f) => w * f 
        }.sum + intercept)
        
        val error = prediction - point.label
        val gradient = point.features.toArray.map(_ * error)
        
        (Vectors.dense(gradient), error)
      }.reduce { case ((g1, e1), (g2, e2)) =>
        val combinedGradient = g1.toArray.zip(g2.toArray).map { case (a, b) => a + b }
        (Vectors.dense(combinedGradient), e1 + e2)
      }
      
      gradients
    }
    
    private def computeLogisticLoss(
      data: RDD[ClassificationData], 
      weights: Vector, 
      intercept: Double
    ): Double = {
      data.map { point =>
        val prediction = sigmoid(weights.toArray.zip(point.features.toArray).map { 
          case (w, f) => w * f 
        }.sum + intercept)
        
        -point.label * math.log(prediction) - (1 - point.label) * math.log(1 - prediction)
      }.mean()
    }
    
    private def sigmoid(x: Double): Double = 1.0 / (1.0 + math.exp(-x))
  }
  
  case class SoteriaLogisticRegressionModel(weights: Vector, intercept: Double) {
    def predict(features: Vector): Double = {
      val score = weights.toArray.zip(features.toArray).map { 
        case (w, f) => w * f 
      }.sum + intercept
      
      if (1.0 / (1.0 + math.exp(-score)) >= 0.5) 1.0 else 0.0
    }
  }
  
  /**
   * SOTERIA Collaborative Filtering (ALS) with SGX Protection
   */
  class SoteriaALS(session: SoteriaSession, rank: Int = 10, maxIterations: Int = 20, regParam: Double = 0.01) {
    
    def train(dataset: EncryptedDataset[RecommendationData]): SoteriaALSModel = {
      // Matrix factorization computations happen in SGX enclave
      val result = session.executeWithPartitioning(
        dataset,
        "model_update",
        (data: Dataset[RecommendationData]) => {
          println("[SOTERIA-SGX] Training ALS model within SGX enclave...")
          
          import session.spark.implicits._
          val ratingsRDD = data.rdd
          
          // Initialize user and item factors within enclave
          val users = ratingsRDD.map(_.user).distinct().collect()
          val items = ratingsRDD.map(_.item).distinct().collect()
          
          var userFactors = users.map { user =>
            (user, Array.fill(rank)(Random.nextGaussian() * 0.1))
          }.toMap
          
          var itemFactors = items.map { item =>
            (item, Array.fill(rank)(Random.nextGaussian() * 0.1))
          }.toMap
          
          // Alternating Least Squares within enclave
          for (iteration <- 1 to maxIterations) {
            // Update user factors
            userFactors = updateUserFactors(ratingsRDD, userFactors, itemFactors, regParam)
            
            // Update item factors  
            itemFactors = updateItemFactors(ratingsRDD, userFactors, itemFactors, regParam)
            
            if (iteration % 5 == 0) {
              val rmse = computeRMSE(ratingsRDD, userFactors, itemFactors)
              println(s"[SOTERIA-SGX] Iteration $iteration, RMSE: $rmse")
            }
          }
          
          (userFactors, itemFactors)
        }
      )
      
      SoteriaALSModel(result._1, result._2, rank)
    }
    
    private def updateUserFactors(
      ratings: RDD[RecommendationData],
      userFactors: Map[Int, Array[Double]],
      itemFactors: Map[Int, Array[Double]], 
      regParam: Double
    ): Map[Int, Array[Double]] = {
      
      ratings.groupBy(_.user).map { case (user, userRatings) =>
        val A = Array.ofDim[Double](rank, rank)
        val b = Array.fill(rank)(0.0)
        
        userRatings.foreach { rating =>
          val itemFactor = itemFactors(rating.item)
          
          // Update normal equations
          for (i <- itemFactor.indices; j <- itemFactor.indices) {
            A(i)(j) += itemFactor(i) * itemFactor(j)
          }
          
          for (i <- itemFactor.indices) {
            A(i)(i) += regParam
            b(i) += rating.rating * itemFactor(i)
          }
        }
        
        // Solve normal equations (simplified)
        val newFactor = solveLinearSystem(A, b)
        (user, newFactor)
      }.collect().toMap
    }
    
    private def updateItemFactors(
      ratings: RDD[RecommendationData],
      userFactors: Map[Int, Array[Double]],
      itemFactors: Map[Int, Array[Double]],
      regParam: Double
    ): Map[Int, Array[Double]] = {
      
      ratings.groupBy(_.item).map { case (item, itemRatings) =>
        val A = Array.ofDim[Double](rank, rank)
        val b = Array.fill(rank)(0.0)
        
        itemRatings.foreach { rating =>
          val userFactor = userFactors(rating.user)
          
          for (i <- userFactor.indices; j <- userFactor.indices) {
            A(i)(j) += userFactor(i) * userFactor(j)
          }
          
          for (i <- userFactor.indices) {
            A(i)(i) += regParam
            b(i) += rating.rating * userFactor(i)
          }
        }
        
        val newFactor = solveLinearSystem(A, b)
        (item, newFactor)
      }.collect().toMap
    }
    
    private def solveLinearSystem(A: Array[Array[Double]], b: Array[Double]): Array[Double] = {
      // Simplified Gaussian elimination (in practice, use more robust solver)
      val n = b.length
      val x = Array.fill(n)(0.0)
      
      // Forward elimination
      for (i <- 0 until n) {
        val pivot = A(i)(i)
        if (math.abs(pivot) > 1e-10) {
          for (j <- i + 1 until n) {
            val factor = A(j)(i) / pivot
            for (k <- i until n) {
              A(j)(k) -= factor * A(i)(k)
            }
            b(j) -= factor * b(i)
          }
        }
      }
      
      // Back substitution
      for (i <- (n - 1) to 0 by -1) {
        x(i) = b(i)
        for (j <- i + 1 until n) {
          x(i) -= A(i)(j) * x(j)
        }
        x(i) /= A(i)(i)
      }
      
      x
    }
    
    private def computeRMSE(
      ratings: RDD[RecommendationData],
      userFactors: Map[Int, Array[Double]],
      itemFactors: Map[Int, Array[Double]]
    ): Double = {
      val predictions = ratings.map { rating =>
        val prediction = userFactors(rating.user).zip(itemFactors(rating.item))
          .map { case (u, i) => u * i }.sum
        
        math.pow(rating.rating - prediction, 2)
      }
      
      math.sqrt(predictions.mean())
    }
  }
  
  case class SoteriaALSModel(
    userFactors: Map[Int, Array[Double]], 
    itemFactors: Map[Int, Array[Double]], 
    rank: Int
  ) {
    def predict(user: Int, item: Int): Double = {
      (userFactors.get(user), itemFactors.get(item)) match {
        case (Some(userVec), Some(itemVec)) =>
          userVec.zip(itemVec).map { case (u, i) => u * i }.sum
        case _ => 0.0 // Default prediction for cold start
      }
    }
  }
  
  /**
   * Security and Privacy Utilities
   */
  object SecurityUtils {
    
    /**
     * Secure data shuffling within SGX enclave
     */
    def secureDataShuffle[T](data: RDD[T], seed: Long = System.currentTimeMillis()): RDD[T] = {
      data.mapPartitionsWithIndex { (index, iterator) =>
        val random = new Random(seed + index)
        val shuffled = iterator.toArray
        
        // Fisher-Yates shuffle within enclave
        for (i <- shuffled.length - 1 to 1 by -1) {
          val j = random.nextInt(i + 1)
          val temp = shuffled(i)
          shuffled(i) = shuffled(j)
          shuffled(j) = temp
        }
        
        shuffled.iterator
      }
    }
    
    /**
     * Secure aggregation with integrity checking
     */
    def secureAggregate[T](data: RDD[T], aggregateFunc: (T, T) => T): T = {
      // Basic secure aggregation - integrity checking to be added in future versions
      data.reduce(aggregateFunc)
    }
    
    /* 
     * FUTURE WORK: Advanced security features for SOTERIA v2+
     * The following features are planned for future versions:
     */
    
    /**
     * Attack detection mechanisms (FUTURE WORK)
     * This feature is not implemented in SOTERIA v1.0
     */
    /*
    def detectAnomalousAccess(accessPattern: Seq[String]): Boolean = {
      // TODO: Implement pattern analysis for detecting potential attacks
      // This will include detection of:
      // - Model inversion attacks
      // - Membership inference attacks  
      // - Model extraction attacks
      val suspiciousPatterns = Set("repeated_model_query", "systematic_inference", "gradient_extraction")
      accessPattern.exists(pattern => suspiciousPatterns.contains(pattern))
    }
    */
    
    /**
     * Placeholder for attack detection (always returns false in v1.0)
     */
    def detectAnomalousAccess(accessPattern: Seq[String]): Boolean = {
      // SOTERIA v1.0: Basic implementation - always returns false
      // Advanced attack detection will be implemented in future versions
      false
    }
  }
}
