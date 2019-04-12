var Config = {
    amazon_s3: {
        user: process.env.S3_API_USER || 's3staging',
        apiKey: process.env.S3_API_KEY || 'AKIAITZQK66KAARQEQPQ',
        apiSecret: process.env.S3_API_SECRET || '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',
        region: process.env.S3_API_REGION || 'us-west-1',
        signatureVersion: process.env.S3_API_SIGNATURE_VERSION || 'v4',
        bucket: addBucketPrefix('default')
    },

    /*
     * A secure s3 instance that is being used for data sensitive content such 
     * as uploaded voucher files.
     */
    secure_s3: {
        user: process.env.S3_API_SECURE_USER || 's3staging',
        apiKey: process.env.S3_API_SECURE_KEY || 'AKIAITZQK66KAARQEQPQ',
        apiSecret: process.env.S3_API_SECURE_SECRET || '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',
        region: process.env.S3_API_SECURE_REGION || 'us-west-1',
        signatureVersion: process.env.S3_API_SIGNATURE_VERSION || 'v4',
        bucket: addBucketPrefix('intern')
    },
    
    buckets: {
        appConfigurator: {
            advertisement: addBucketPrefix('advertisement'),
            blob: addBucketPrefix('encrypted')
        },
        
        question: {
            blobInput: addBucketPrefix('medias'),
            blobOutput: addBucketPrefix('encrypted')
        },
        
        medias: {
            medias: addBucketPrefix('medias')
        }
    },
    
    mediaConfiguration: {
        /*
         * Files are assigned to different namespaces, used as a folder in the filename:
         *     voucher-big/1111-222-333_high.jpg
         */
        namespace: {
            'app': [
                {
                    'resolution': '1080x1440',
                    'label': 'xhigh'
                },
                {
                    'resolution': '800x1067',
                    'label': 'high'
                },
                {
                    'resolution': '640x853',
                    'label': 'medium'
                },
                {
                    'resolution': '480x640',
                    'label': 'low'
                }
            ],
            'language': [
                // No thumbnail, will store only the original file size
            ],
            'voucher': [
                {
                    'resolution': '1080x473',
                    'label': 'xhigh'
                },
                {
                    'resolution': '800x350',
                    'label': 'high'
                },
                {
                    'resolution': '640x280',
                    'label': 'medium'
                },
                {
                    'resolution': '480x210',
                    'label': 'low'
                }
            ],
            'voucherBig': [
                {
                    'resolution': '1080x1440',
                    'label': 'xhigh'
                },
                {
                    'resolution': '800x1067',
                    'label': 'high'
                },
                {
                    'resolution': '640x853',
                    'label': 'medium'
                },
                {
                    'resolution': '480x640',
                    'label': 'low'
                }
            ],
            'profile': [
                {
                    'resolution': '216x216',
                    'label': 'xhigh'
                },
                {
                    'resolution': '160x160',
                    'label': 'high'
                },
                {
                    'resolution': '128x128',
                    'label': 'medium'
                },
                {
                    'resolution': '96x96',
                    'label': 'low'
                }
            ],
            'question': [
                {
                    'resolution': '1080x1440',
                    'label': 'xhigh'
                },
                {
                    'resolution': '800x1067',
                    'label': 'high'
                },
                {
                    'resolution': '640x853',
                    'label': 'medium'
                },
                {
                    'resolution': '480x640',
                    'label': 'low'
                }
            ],
            'answer': [
                {
                    'resolution': '432x432',
                    'label': 'xhigh'
                },
                {
                    'resolution': '320x320',
                    'label': 'high'
                },
                {
                    'resolution': '256x256',
                    'label': 'medium'
                },
                {
                    'resolution': '192x192',
                    'label': 'low'
                }
            ],
            'game': [
                {
                    'resolution': '1080x1440',
                    'label': 'xhigh'
                },
                {
                    'resolution': '800x1067',
                    'label': 'high'
                },
                {
                    'resolution': '640x853',
                    'label': 'medium'
                },
                {
                    'resolution': '480x640',
                    'label': 'low'
                }
            ],
            'gameType': [
                {
                    'resolution': '324x324',
                    'label': 'xhigh'
                },
                {
                    'resolution': '240x240',
                    'label': 'high'
                },
                {
                    'resolution': '192x192',
                    'label': 'medium'
                },
                {
                    'resolution': '144x144',
                    'label': 'low'
                }
            ],
            'ad': [
                {
                    'resolution': '1080x1440',
                    'label': 'xhigh'
                },
                {
                    'resolution': '800x1067',
                    'label': 'high'
                },
                {
                    'resolution': '640x853',
                    'label': 'medium'
                },
                {
                    'resolution': '480x640',
                    'label': 'low'
                }
            ],
            'tombola': [
                {
                    'resolution': '1080x473',
                    'label': 'xhigh'
                },
                {
                    'resolution': '800x350',
                    'label': 'high'
                },
                {
                    'resolution': '640x280',
                    'label': 'medium'
                },
                {
                    'resolution': '480x210',
                    'label': 'low'
                }
            ],
            'tombolaBundles': [
                {
                    'resolution': '216x216',
                    'label': 'xhigh'
                },
                {
                    'resolution': '160x160',
                    'label': 'high'
                },
                {
                    'resolution': '128x128',
                    'label': 'medium'
                },
                {
                    'resolution': '96x96',
                    'label': 'low'
                }
            ],
            'promo': [
                {
                    'resolution': '1080x1440',
                    'label': 'xhigh'
                },
                {
                    'resolution': '800x1067',
                    'label': 'high'
                },
                {
                    'resolution': '640x853',
                    'label': 'medium'
                },
                {
                    'resolution': '480x640',
                    'label': 'low'
                }
            ],
            'pools': [
                {
                    'resolution': '216x216',
                    'label': 'xhigh'
                },
                {
                    'resolution': '160x160',
                    'label': 'high'
                },
                {
                    'resolution': '128x128',
                    'label': 'medium'
                },
                {
                    'resolution': '96x96',
                    'label': 'low'
                }
            ],
            'tenantLogo': [
                {
                    'resolution': '432x432',
                    'label': 'xhigh'
                },
                {
                    'resolution': '320x320',
                    'label': 'high'
                },
                {
                    'resolution': '256x256',
                    'label': 'medium'
                },
                {
                    'resolution': '192x192',
                    'label': 'low'
                }
            ],
            'icon': [
                {
                    'resolution': '324x324',
                    'label': 'xhigh'
                },
                {
                    'resolution': '240x240',
                    'label': 'high'
                },
                {
                    'resolution': '192x192',
                    'label': 'medium'
                },
                {
                    'resolution': '144x144',
                    'label': 'low'
                }
            ],
            'shopArticle': [
                {
                    'resolution': '1000x1000',
                    'label': 'high'
                },
                {
                    'resolution': '400x400',
                    'label': 'medium'
                },
                {
                    'resolution': '200x200',
                    'label': 'low'
                }
            ],
            'intro': [
                {
                    'resolution': '1080x1440',
                    'label': 'xhigh'
                },
                {
                    'resolution': '800x1067',
                    'label': 'high'
                },
                {
                    'resolution': '640x853',
                    'label': 'medium'
                },
                {
                    'resolution': '480x640',
                    'label': 'low'
                }
            ]
        },
        /*
         * Type of images supported, the imagemagick image library is used for image processing.
         * Other file formats are not supported and not accepted.
         */
        imageTypes: ['jpg', 'jpeg', 'png', 'gif'],
        /*
         * The format template is used to format the uploaded image file,
         *    Ex: 1111-222-333_300x120.jpg
         */
        fileFormatTemplate: '%BASENAME%_%LABEL%%EXTENSION%',
        imageQuality: 100, //percentage - use below 100 for reducing quality and image size
        imageProcessingQueueSize: 20, //maximum gm processes to run simultaneously
        imageProcessingQueueTimeout: 10000, //queue worker timeout - to allow queue move forward
    },
    
    addBucketPrefix: addBucketPrefix
};

function addBucketPrefix (bucketName) {
    return (process.env.S3_API_BUCKET_PREFIX || 'f4m-nightly-') + bucketName;
};

module.exports = Config;

