module.exports = {
    APP_1: {
        "tenant": {
            "tenantId": 1
        },
        "application": {
            "id": 1,
            "tenantId": 1,
            "configuration": {
                "menu_color": {
                    "appArea": "121212"
                },
                "menu_picture": {
                    "appArea": "12345678qwerty.jpeg"
                },
                "referral_tearing": {
                    "10": {
                        "bonusPoints": 1,
                        "credits": 1
                    }
                },
                "boxStype": "character",
                "moneyGames": false,
                "minimumEntryFee": 100,
                "maximumEntryFee": 1000,
                "gamblingComponent": false,
                "paymentNeeded": true,
                "onlyAnonymous": true,
                "minimumGameLevel": 3,
                "pushNotification": true,
                "internationalGames": true,
                "firstTimeSignUpBonusPoints": 10,
                "firstTimeSignUpCredits": 5,
                "referralBonusPoints": 30,
                "referralCredits": 10,
                "referalProgram": {
                    "bonuspoints": 10,
                    "credits": 5,
                    "tiering": [
                        {
                            "quantityMin": 1,
                            "credits": 10,
                            "bonuspoints": 5
                        }
                    ]
                }
            },
            "deployment": {
                "android": {
                    "name": "Android deployment",
                    "serviceVersion": "v.0.0.1",
                    "packageName": "org.f4m.application1",
                    "usesFeatureString": "camera",
                    "versionCode": "1",
                    "packageSize": 1234,
                    "targetPlattform": "Android",
                    "targetPublisher": "unknown",
                    "targetMarket": "google play",
                    "typeOfRelease": "Alpha",
                    "targetFTPServer": "host",
                    "targetFTPUser": "user",
                    "targetFTPPassword": "qwerty",
                    "targetFTPKey": "1234",
                    "targetFTPProtocol": "ftp",
                    "targetFTPAuthType": "anonymous"
                },
                "ios": {
                    "name": "iOS deployment",
                    "serviceVersion": "v.0.0.1",
                    "packageName": "org.f4m.application1",
                    "usesFeatureString": "camera",
                    "versionCode": "1",
                    "packageSize": 1234,
                    "targetPlattform": "iOS",
                    "targetPublisher": "unknown",
                    "targetMarket": "iTunes",
                    "typeOfRelease": "Alpha",
                    "targetFTPServer": "host",
                    "targetFTPUser": "user",
                    "targetFTPPassword": "qwerty",
                    "targetFTPKey": "1234",
                    "targetFTPProtocol": "ftp",
                    "targetFTPAuthType": "anonymous"
                },
                "web": null
            },
            "isDeployed": 0,
            "title": "Application one title",
            "status": "inactive",
            "description": "Application one description",
            "releaseDate": "2016-11-28T20:48:51",
            "createDate": "2016-11-28T20:48:51",
            "applicationAnimations": [
                {
                    "id": 1,
                    "name": "Animation one",
                    "description": "Animation one descritpion",
                    "areaUsage": "Area usage one",
                    "applicationHasApplicationAnimations": [
                        {
                            "applicationId": 1,
                            "applicationAnimationId": 1,
                            "status": "active"
                        }
                    ]
                }
            ],
            "applicationCharacters": [
                {
                    "id": 1,
                    "name": "Character one",
                    "description": "Character one description",
                    "applicationHasApplicationCharacters": [
                        {
                            "applicationId": 1,
                            "applicationCharacterId": 1,
                            "status": "active"
                        }
                    ]
                }
            ],
            "paymentTypes": [
                {
                    "id": 1,
                    "name": "Payment type one",
                    "applicationHasPaymentTypes": [
                        {
                            "applicationId": 1,
                            "paymentTypeId": 1
                        }
                    ]
                }
            ]
        },
        "friendsForMedia": {},
    }
}
