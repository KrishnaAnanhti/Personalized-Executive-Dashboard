# Personalized-Executive-Dashboard
Personalized Executive Dashboard is an android mobile application used by executives to review the daily operational dashboard and take necessary action if required. It is built using Lex, Polly, Rekognition, API Gateway, Lambda, Cognito and s3.

Profile Images of the Executives are stored in s3 bucket along with metadata like name,role, region, associated cities. Executive opens the application and takes his picture. Then the captured image is compared with prepopulated images in S3 using Rekognition. If the image doesn't match with images in S3 bucket, then jarvis displays an animated image of captured picture(Alternative Flow). If the image matched(Normal Flow) then jarvis(we have named the lex chatbot as jarvis) welcomes the executive and ask for password(VikingsRock,Unisysstarone or howdy) for confirmation. Once confirmed the daily operational dashboard is displayed to the Executive for review and profile of matched image is shown along with smiley which is based on emotion detected from captured live image. Then jarvis helps to open up weather details in texas region. When there is a alert, jarvis inform the executive and get the confirmation for creating a watch ticket. Then executive can check the new ticket created and assigned to a worker.

Device Specification:
Operating System : Android 6.0+ (Marshmallow or Nougat) Tested using Lenovo Yoga tablet, Samsung Galaxy tablets. Works in almost all tablets.


normal flow : https://www.youtube.com/watch?v=fYxVWlseL9Y
alternate flow :  https://www.youtube.com/watch?v=BpuVEgqOWT8
