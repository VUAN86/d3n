# Backend-java

For DevOps, QA, DEV, Support

> This project covering next needs:

- 1.Source code for java-backend

- 2.Build and deploy java-backend services with dapp, such as:

> - Achievement-service
> - Promocode-service
> - Tombola-service
> - Voucher-service
> - Workflow-service
> - Friend-manager-service
> - Profile-service
> - User-message-service
> - Game-engine-service
> - Game-selection-service
> - Result-engine-service
> - Winning-service
> - Advertisement-service
> - Event-service
> - Payment-service
> - Registry-service

For succesfull build and push images from Dapp need:

> Tree view:
```bash
>├── .dappfiles
>│   └── dimg
>│       └── $servicename.tmpl
>|   └── files
>|       └── java.tmpl        
>|
>├── dappfile.yaml
>|
>└── README.md
```

- java.tml contains vars for dimg images
- $servicename.tmpl - contains dimg of all java services, where name "$servicename" is name of service(like promocode-service.tmpl)


