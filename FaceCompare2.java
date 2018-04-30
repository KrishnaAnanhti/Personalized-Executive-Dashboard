package com.unisys.demo;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.CompareFacesMatch;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.ComparedFace;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.ObjectMapper; 
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FaceCompare2 implements Callable<String>{
	public static final AWSCredentials credentials = new BasicAWSCredentials("AKIAJPENKHNP7XQKMBJA",
			"J1yOEHPe0qZ5PKaNq1VgnGCQ7XNrcpPM3cTJOnwq");
	public static final AmazonS3 s3client = new AmazonS3Client(credentials);
	public static final String sampleBucket = "rekognition-face-comparsion";
	public static final String testBucket = "rekognition-face-comparison-test";
	public static Image source;
	public static String targetImage;
	public String target;
	
	public FaceCompare2() {  
	}
	public FaceCompare2(String target) { 
		this.target=target;
	}

	 public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	@Override
	public String call() throws Exception {  
		 System.out.println(this.getTarget());
		 String targ=this.getTarget();
		 Image target = getImageUtil(sampleBucket, targ);
			Float similarityThreshold = 70F;
			AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_EAST_1)
					.withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
			CompareFacesResult compareFacesResult = callCompareFaces(source, target, similarityThreshold,
					rekognitionClient);

			List<CompareFacesMatch> faceDetails = compareFacesResult.getFaceMatches();
			if (faceDetails.size() == 0)
				System.out.println("No match " + source.getS3Object().getName() + " " + targ);
			for (CompareFacesMatch match : faceDetails) {
				ComparedFace face = match.getFace();
				BoundingBox position = face.getBoundingBox();
				System.out.println(
						source.getS3Object().getName() + " " + targ + " \n Face at " + position.getLeft().toString() + " "
								+ position.getTop() + " matches with " + face.getConfidence().toString() + "% confidence.");
				return "rek" + targ + "rek" + face.getConfidence().toString();
			}
			return "rek"; 
	} 
	public String myHandler(OutputStream outputStream, Context context) {
		LambdaLogger logger = context.getLogger();
		logger.log("\nStarting");
		String returnStr = "";
		String matchedImage = compareImages();
		JSONObject faceAnalysis;
		try {
			if (matchedImage == "") {
				faceAnalysis = new JSONObject();
				System.out.println("Image does not match");
				returnStr = getFacialData(targetImage, "NoMatch");
				// faceAnalysis.put("statusMsg", "NotMatched");
				// outputStream.write(faceAnalysis.toString().getBytes(Charset.forName("UTF-8")));
				// return faceAnalysis.toString();
			} else{
				returnStr = getFacialData(targetImage, matchedImage);
				deleteFromS3(targetImage);
			}
			outputStream.write(returnStr.getBytes(Charset.forName("UTF-8")));
			System.out.println(returnStr);
			return returnStr;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.log("error : " + e.getMessage());
		}
		return returnStr;
	}
	private static void deleteFromS3(String targetKey) {
		 try {
			 s3client.deleteObject(new DeleteObjectRequest(testBucket, targetKey));
	        } catch (AmazonServiceException ase) {
	            System.out.println("Caught an AmazonServiceException.");
	            System.out.println("Error Message:    " + ase.getMessage());
	            System.out.println("HTTP Status Code: " + ase.getStatusCode());
	            System.out.println("AWS Error Code:   " + ase.getErrorCode());
	            System.out.println("Error Type:       " + ase.getErrorType());
	            System.out.println("Request ID:       " + ase.getRequestId());
	        } catch (AmazonClientException ace) {
	            System.out.println("Caught an AmazonClientException.");
	            System.out.println("Error Message: " + ace.getMessage());
	        }
		
	}
	private static String getFacialData(String targetKey, String oldKey) {
		ObjectMetadata objMeta = new ObjectMetadata();
		if (!oldKey.equals("NoMatch"))
			objMeta = s3client.getObjectMetadata(sampleBucket, oldKey);
		JSONObject json = null;
		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_EAST_1)
				.withCredentials(new AWSStaticCredentialsProvider(credentials)).build();

		DetectFacesRequest request = new DetectFacesRequest()
				.withImage(new Image().withS3Object(new S3Object().withName(targetKey).withBucket(testBucket)))
				.withAttributes(Attribute.ALL);
		try {
			DetectFacesResult result = rekognitionClient.detectFaces(request);
			List<FaceDetail> faceDetails = result.getFaceDetails();
			if(faceDetails.size()==0){
				json= new JSONObject();
				json.put("statusMsg", "RandomImage");
				return json.toString();
			}
			for (FaceDetail face : faceDetails) {
				ObjectMapper objectMapper = new ObjectMapper();
				String facialStr = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(face);
				json = new JSONObject(facialStr);
				json.remove("landmarks");
				json.remove("boundingBox");
				json.remove("sunglasses");
				json.remove("gender");
				json.remove("beard");
				json.remove("mustache");
				json.remove("eyesOpen");
				json.remove("mouthOpen");
				json.remove("landmarks");
				json.remove("pose");
				json.remove("quality");
				if (!oldKey.equals("NoMatch")){ 
					json.put("personFirstName", objMeta.getUserMetaDataOf("firstName"));
					json.put("personLastName", objMeta.getUserMetaDataOf("lastName"));
					json.put("statusMsg", "Matched");
					json.put("role", objMeta.getUserMetaDataOf("role"));
					json.put("gender", objMeta.getUserMetaDataOf("gender"));
					json.put("matchedImage", oldKey);
					json.put("city", objMeta.getUserMetaDataOf("city"));
				}
				else
				{
					json.put("statusMsg", "NotMatched");
				}	
				return json.toString();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return json.toString();
	}

	private static String compareImages() {
		String matchedImage = "", returnedString, target;
		int score, higherScore = 0;
		try {
			// fetch the recently uploaded image from test bucket
			String sourceImg = getSourceImage();
			if (sourceImg == null || sourceImg == "")
				System.out.println("Test bucket is empty..try uploading your image again");
			source = getImageUtil(testBucket, sourceImg);
			// fetch the image from existing sample bucket
			ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(sampleBucket);
			ObjectListing objectListing;
			ExecutorService executor = Executors.newFixedThreadPool(10); 
	        List<Future<String>> list = new ArrayList<Future<String>>(); 
			
			do {
				objectListing = s3client.listObjects(listObjectsRequest);
				for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
					// compare source with target images
					  Callable<String> callable = new FaceCompare2(objectSummary.getKey()); 
			          Future<String> future = executor.submit(callable);
			          list.add(future); 
				}
				listObjectsRequest.setMarker(objectListing.getNextMarker());
			} while (objectListing.isTruncated());
			 for(Future<String> fut : list){
		                System.out.println("::"+fut.get());
		                returnedString=fut.get();
		                String[] returnArray = returnedString.split("rek");
						System.out.println(returnArray.length);
						if (returnArray.length > 0) {
							target = returnArray[1];
							double d = Double.parseDouble(returnArray[2]);
							score = (int) d;
							if (higherScore < score) {
								higherScore = score;
								matchedImage = target;
							}
						} 
		        }
			System.out.println("matchedImage" + matchedImage);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
		if (matchedImage == "")
			return "";
		return matchedImage;
	}

	private static String getSourceImage() {
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(testBucket);
		ObjectListing objectListing;
		String recentImage = "";
		do {
			objectListing = s3client.listObjects(listObjectsRequest);
			Date recentDate = null;
			for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
				Date imgDate = objectSummary.getLastModified();
				if (recentDate == null)
					recentDate = imgDate;
				if (recentDate.before(imgDate)) {
					System.out.println(recentDate + " is before " + imgDate);
					recentDate = imgDate;
					recentImage = objectSummary.getKey();
				}
				if (recentDate.equals(imgDate)) {
					System.out.println(recentDate + " is equals " + imgDate);
					recentDate = imgDate;
					recentImage = objectSummary.getKey();
				}
			}
			System.out.println("recent date " + recentDate + "  recent image " + recentImage);
			listObjectsRequest.setMarker(objectListing.getNextMarker());
		} while (objectListing.isTruncated());
		targetImage = recentImage;
		return recentImage;
	} 

	private static CompareFacesResult callCompareFaces(Image sourceImage, Image targetImage, Float similarityThreshold,
			AmazonRekognition amazonRekognition) {

		CompareFacesRequest compareFacesRequest = new CompareFacesRequest().withSourceImage(sourceImage)
				.withTargetImage(targetImage).withSimilarityThreshold(similarityThreshold);
		return amazonRekognition.compareFaces(compareFacesRequest);
	}

	private static Image getImageUtil(String bucket, String key) {
		return new Image().withS3Object(new S3Object().withBucket(bucket).withName(key));
	}
}
