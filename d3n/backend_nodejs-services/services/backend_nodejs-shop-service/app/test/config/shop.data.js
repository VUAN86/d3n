var DataIds = require('./_id.data.js');

module.exports = {
    ARTICLE_ADD: {
        id: 'testid1',
        parentId: 'testparentid',
        articleNumber: 'testnumber',
        title: 'testtitle',
        categoryId: DataIds.CATEGORY_ID,
        moneyPrice: "122",
        bonusPrice: "10",
        fullMoneyPrice: "150",
        imageURLs: [ "img1", "img2" ],
        description: "testdescription",
        stock: '101',
        active: "1",
        //minDeliveryTime: "1",
        //maxDeliveryTime: "2",
        //deliveryTimeUnit: "DAY"
    },

    ARTICLE_ADD_INVALID:{
        invalid: "",
    },

    ARTICLE_UPDATE: {
        id: 'testid1',
        fullMoneyPrice: "200",
    },

    ARTICLE_DELETE: {
        id: 'testid1'
    },
    TENANT_ADD: {
        id: 'testid1',
        title: 'testtitle',
        description: 'testdescription',
        iconURL: 'testicon',
        termsURL: 'testterms',
        street: "teststreet",
        streetNumber: "1",
        zip: "testzip",
        city: 'testcity',
        country: 'DE',
        phone: "testphone",
        email: "testemail",
        taxNumber: "testtaxnumber",
    },

    TENANT_ADD_INVALID:{
        invalid: "",
    },

    TENANT_UPDATE: {
        id: 'testid1',
        streetNumber: "2"
    },

    TENANT_DELETE: {
        id: 'testid1'
    }
}