var _ = require('lodash'),
    JwtUtils = require('./../../../utils/jwtUtils.js'),
    PassportFactory = require('./../factories/passportFactory.js');

module.exports = function (app, passport) {
    app.get('/', function (req, res) {
        var local = app.get('local'),
            facebook = app.get('facebook'),
            google = app.get('google');
        res.render('index.ejs', {
            message: req.flash('indexMessage'),
            local: local,
            facebook: facebook,
            google: google,
        });
	});

    app.get('/login', function (req, res) {
        res.render('login.ejs', {
            message: req.flash('loginMessage'),
        });
	});
    
    app.post('/auth/local', passport.authenticate('local-login', {
        successRedirect: '/profile',
        failureRedirect: '/login',
        failureFlash: true
    }));

    app.get('/auth/facebook', passport.authenticate('facebook', {
        scope: ['email']
    }));
    
    app.get('/auth/facebook/callback', passport.authenticate('facebook', {
        successRedirect: '/profile',
        failureRedirect: '/',
        failureFlash: true
    }));

    app.get('/auth/google', passport.authenticate('google', {
        scope: ['profile', 'email']
    }));
    
    app.get('/auth/google/callback', passport.authenticate('google', {
        successRedirect: '/profile',
        failureRedirect: '/',
        failureFlash: true
    }));
    
    app.get('/auth/refresh/:provider(local|facebook|google)', function (req, res) {
        var cfg = app.get(req.params.provider);
        PassportFactory.refresh(req, req.params.provider, cfg, function (err, data) {
            if (err) {
                app.set(req.params.provider, undefined);
            } else {
                cfg.token = data.token;
                cfg.authTill = formatTill(data.exp);
                app.set(req.params.provider, cfg);
            }
            res.redirect('/');
        });
    });
    
    app.get('/changePassword', function (req, res) {
        res.render('newpass.ejs', {
            message: req.flash('loginMessage')
        });
    });
    
    app.post('/auth/changePassword', function (req, res) {
        var cfg = app.get('local');
        PassportFactory.changePassword(req, cfg, function (err, data) {
            if (err) {
                app.set('local', undefined);
            }
            res.redirect('/');
        });
    });
    
    app.get('/profile', isLoggedIn, function (req, res) {
        var data = app.get(req.user.provider);
        if (_.isUndefined(data)) {
            app.set(req.user.provider, {
                userid: req.user.payload.userid,
                token: req.user.token,
                authTill: formatTill(req.user.payload.exp)
            });
        } else {
            req.user.token = data.token;
            req.user.payload = JwtUtils.decode(data.token).payload;
        }
        res.render('profile.ejs', { user: req.user });
    });
    
    app.get('/logout', function (req, res) {
        app.set('local', undefined);
        app.set('facebook', undefined);
        app.set('google', undefined);
		req.logout();
		res.redirect('/');
	})
};

function isLoggedIn(req, res, next) {
	if(req.isAuthenticated()) {
		return next();
	}

	res.redirect('/login');
}

function formatTill(seconds) {
    var date = new Date(seconds * 1000);
    return 'Till ' + leftPad(date.getHours(), 2) + ':' + leftPad(date.getMinutes(), 2) + ':' + leftPad(date.getSeconds(), 2);
}

function leftPad(number, targetLength) {
    var output = number + '';
    while (output.length < targetLength) {
        output = '0' + output;
    }
    return output;
}