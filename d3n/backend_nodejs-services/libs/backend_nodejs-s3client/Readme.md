# About

Library that interfaces with Amazon S3 storage, provides a set of api's to work with it

# Howto
For initialization you can either provide a set of configuration settings or use the defaults which are setup via environment variables.

    var S3client = require('nodejs-s3client');
    var Storage = S3client.getInstance(configuration);

**Configuration** object that needs to be injected:

    amazon_s3:
        user: env.S3_API_USER
        apiKey: env.S3_API_KEY
        apiSecret: env.S3_API_SECRET
        region: env.S3_API_REGION
        bucket: env.S3_API_BUCKET

# API Calls

### getObjectAsBuffer(args, cb)

Return the S3 object id as specified in *args.objectId* as a buffer.

### getObject(args, cb)

Read S3 object *args.objectId* and write it into *args.outFile*.

### deleteObjects(args, cb)

Delete an array of S3 object ids provided in *args.objectKeys*.

### uploadObject(objectId, objectStream, cbProgress, cbDone)
Upload an object to S3 storage with **objectId** using the **objectStream** as the
data source. The **cbProgress** callback is optional, **cbDone** is called once
the upload to S3 completes.

### listBuckets(cb)

List buckets available.

### listObjects(params, cb)

List objects available in the default bucket. Supported params can be seen [here](http://docs.aws.amazon.com/AWSJavaScriptSDK/latest/AWS/S3.html#listObjectsV2-property).

Some defaults are setup for you:
* Bucket - if not specified the default from configuration is used
* MaxKeys - default is 100

### createBucketIfNotExists(bucketName, cb)

Create *bucketName* if it does not already exists.
