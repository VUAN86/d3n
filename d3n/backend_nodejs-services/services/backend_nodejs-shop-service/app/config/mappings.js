var Mappings = {
    "Category": {
        "oxid": "id",
        "oxparentid": "parentId",
        "oxright": "right",
        "oxleft": "left",
        "oxrootid": "rootId",
        "oxsort": "sort",
        "oxtitle": "title",
        "oxdesc": "description",
        "oxlongdesc": "longDescription",
        "oxthumb": "thumbURL"
    },
    "Article": {
        "oxid": "id",
        "oxparentid": "parentId",
        "oxartnum": "articleNumber",
        "oxtitle": "title",
        "oxbonuspricemoney": "moneyPrice",
        "oxbonuspricepoints": "bonusPrice",
        "oxprice": "fullMoneyPrice",
        "oxbonuspricecredits": "creditPrice",
        "oxshortdesc": "description",
        "oxstock": "stock",
        "oxactive": "active",
        "oxmindeltime": "minDeliveryTime",
        "oxmaxdeltime": "maxDeliveryTime",
        "oxdeltimeunit": "deliveryTimeUnit",
        "oxvendorid": "tenantId"
    },
    "Tenant": {
        "oxid": "id",
        "oxtitle": "title",
        "oxshortdesc": "description",
        "oxicon": "iconURL",
        "oxterms": "termsURL",
        "oxstreet": "street",
        "oxstreetnr": "streetNumber",
        "oxzip": "zip",
        "oxcity": "city",
        "oxcountryid": "country",
        "oxphone": "phone",
        "oxemail": "email",
        "oxtaxnr": "taxNumber"
    },
    "Order": {
        "oxid": "id",
        "oxordernr": "orderNumber",
        "oxorderdate": "createDate",
        "oxsenddate": "finalizeDate",
        "oxapprover": "finalizeUserId"
    },
    "TransactionInfo": {
        "oxmoneytransactionid": "money",
        "oxbonustransactionid": "bonus",
        "oxcreditstransactionid": "credit"
    },
    "BillingAddress": {
        "oxbillfname": "firstName",
        "oxbilllname": "lastName",
        "oxbillemail": "email",
        "oxbillstreet": "street",
        "oxbillstreetnr": "streetNumber",
        "oxbillcity": "city",
        "oxbillcountryid": "country",
        "oxbillzip": "zip"
    },
    "ShippingAddress": {
        "oxdelfname": "firstName",
        "oxdellname": "lastName",
        "oxdelstreet": "street",
        "oxdelstreetnr": "streetNumber",
        "oxdelcity": "city",
        "oxdelcountryid": "country",
        "oxdelzip": "zip"
    },
    "List": {
        "numObjects": "total",
        "result": "items"
    },
    "ProfilePerson": {
        "firstName": "firstName",
        "lastName": "lastName"
    },
    "ProfileAddress": {
        "street": "street",
        //"streetNumber": "streetNumber", --> seems it was removed (but not from schema :p)
        "city": "city",
        "country": "country",
        "postalCode": "zip"
    },
    "ShopAddress": {
        "firstName": "oxfname",
        "lastName": "oxlname",
        "street": "oxstreet",
        "streetNumber": "oxstreetnr",
        "city": "oxcity",
        "country": "oxcountryid",
        "zip": "oxzip"
    },
    "CompleteOrder": {
        "money": "oxmoneytransactionid",
        "bonus": "oxbonustransactionid",
        "credit": "oxcreditstransactionid"
    }
}

module.exports = Mappings;