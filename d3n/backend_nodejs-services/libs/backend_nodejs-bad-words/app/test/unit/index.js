var assert = require('assert');
var BadWords = require('../../../index.js');

describe('test BadWords class', function () {

    it('load bad words', function (done) {
        var badWords = new BadWords();
        badWords._loadBadWords(done);
    });

    it('contain bad word at begining', function (done) {
        var badWords = new BadWords();
        var entity = {
            name: 'stupid question',
            title: 'a good title'
        };

        badWords.hasBadWords(entity, ['name', 'title'], function (err, result) {
            try {
                assert.ifError(err);
                assert.strictEqual(result, true);
                done();
            } catch (e) {
                done(e);
            }
        });
    });
    it('contain bad word at end', function (done) {
        var badWords = new BadWords();
        var entity = {
            name: 'asda asd',
            title: 'question title is stupid'
        };

        badWords.hasBadWords(entity, ['name', 'title'], function (err, result) {
            try {
                assert.ifError(err);
                assert.strictEqual(result, true);
                done();
            } catch (e) {
                done(e);
            }
        });
    });
    it('contain bad word in middle', function (done) {
        var badWords = new BadWords();
        var entity = {
            name: 'asda asd',
            title: 'a stupid    question title'
        };

        badWords.hasBadWords(entity, ['name', 'title'], function (err, result) {
            try {
                assert.ifError(err);
                assert.strictEqual(result, true);
                done();
            } catch (e) {
                done(e);
            }
        });
    });
    it('contain bad word uppercase', function (done) {
        var badWords = new BadWords();
        var entity = {
            name: 'asda asd',
            title: 'a STUpid    question title'
        };

        badWords.hasBadWords(entity, ['name', 'title'], function (err, result) {
            try {
                assert.ifError(err);
                assert.strictEqual(result, true);
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('not contain bad words', function (done) {
        var badWords = new BadWords();
        var entity = {
            name: 'a good name',
            title: 'a good title'
        };

        badWords.hasBadWords(entity, ['name', 'title'], function (err, result) {
            try {
                assert.ifError(err);
                assert.strictEqual(result, false);
                done();
            } catch (e) {
                done(e);
            }
        });
    });
    it('contain bad word but not as a separate word', function (done) {
        var badWords = new BadWords();
        var entity = {
            name: 'a aaastupidbbb sadasd',
            title: 'a good title'
        };

        badWords.hasBadWords(entity, ['name', 'title'], function (err, result) {
            try {
                assert.ifError(err);
                assert.strictEqual(result, false);
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('not contain bad words, not strings', function (done) {
        var badWords = new BadWords();
        var entity = {
            active: true,
            tenantId: 1234
        };

        badWords.hasBadWords(entity, ['active', 'tenantId'], function (err, result) {
            try {
                assert.ifError(err);
                assert.strictEqual(result, false);
                done();
            } catch (e) {
                done(e);
            }
        });
    });

});
