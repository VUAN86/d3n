module.exports = {
    TOMBOLA_1: {
        id: 1,
        name: 'Tombola 1',
        description: 'Info',
        winningRules: 'Winning rule',
        imageId: '12345.jpg',
        startDate: '2017-01-04T16:07:43Z',
        endDate: '2017-10-04T16:07:44Z',
        status: 'active',
        playoutTarget: 'PERCENT_OF_TICKETS_USED',
        targetDate: null,
        percentOfTicketsAmount: 50,
        applicationsIds: ['1', '2'],
        regionalSettingsIds: ['1', '2'],
        totalTicketsAmount: 10,
        bundles: [
            {
                'amount': 10,
                'price': 100,
                'imageId': '12345.jpg'
            }
        ],
        prizes: [
            {
                'id': '1-1',
                'type': 'CREDITS',
                'amount': 10,
                'imageId': '12345.jpg',
                'voucherId': '12345',
                'draws': 2
            }
        ],
        consolationPrize:
        {
            'id': '1-2',
            'name': 'Prize',
            'type': 'CREDIT',
            'amount': 10,
            'imageId': '12345.jpg',
            'voucherId': '12345',
            'draws': 2
        }
    },
    TOMBOLA_2: {
        id: 2,
        name: 'Tombola 2',
        description: 'Info',
        winningRules: 'Winning rule',
        imageId: '12345.jpg',
        startDate: '2017-02-04T16:07:43Z',
        endDate: '2017-06-04T16:07:44Z',
        status: 'active',
        playoutTarget: 'TARGET_DATE',
        targetDate: '2017-05-01T18:00:00Z',
        percentOfTicketsAmount: 50,
        applicationsIds: ['2', '3'],
        regionalSettingsIds: ['2', '3'],
        totalTicketsAmount: 100,
        bundles: [
            {
                'amount': 10,
                'price': 100,
                'imageId': '12345.jpg'
            }
        ],
        prizes: [
            {
                'id': '2-1',
                'type': 'CREDITS',
                'amount': 10,
                'imageId': '12345.jpg',
                'voucherId': '12345',
                'draws': 2
            }
        ],
        consolationPrize:
        {
            'id': '2-2',
            'name': 'Prize',
            'type': 'CREDIT',
            'amount': 10,
            'imageId': '12345.jpg',
            'voucherId': '12345',
            'draws': 2
        }        
    }
}
