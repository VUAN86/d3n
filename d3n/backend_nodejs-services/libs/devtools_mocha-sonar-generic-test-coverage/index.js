var fs = require('fs'),
		util = require('util');
var path = require('path');
var mkdirp = require('mkdirp');

var result = "";

module.exports = function (runner, options) {
	if (!options.reporterOptions) { options.reporterOptions = {}; }
	var mochaFile = options.reporterOptions.mochaFile || process.env.MOCHA_FILE || 'test-results.xml';
	
	var stack = {};
	var title;
	
	runner.on('start', function() {
		if (fs.existsSync(mochaFile)) {
		  fs.unlinkSync(mochaFile);
		}
	});
  
	runner.on('test end', function(test){
		var file = test.file.substr(test.file.indexOf(process.cwd()) + process.cwd().length + 1);
		stackF = stack[file];
		if(!stackF){
			stackF = stack[file] = [];
		}
		var mtest = {
			title: test.title,
			titleId: title + ': ' + test.title,
			suite: title,
			stack: test.stack,
			message: test.message,
			file: file,
			duration: test.duration,
			state: test.state != undefined ? test.state : 'skipped'
		};
		stackF.push(mtest);
	});
	
	runner.on('suite', function(test){
		title = test.title;
	});

	runner.on('fail', function(test, err){
		test.stack = err.stack;
		test.message = err.message;
	});

	runner.on('end', function() {
		append('<unitTest version="1">');
		Object.keys(stack).forEach(function(file){
			append(util.format('	<file path="%s">', file));
			stack[file].forEach(function(test){
				switch(test.state){
					case 'passed':
						append(util.format(
							'		<testCase name="%s" duration="%d"/>',
							espape(test.titleId), test.duration
						));
						break;
					default :
						append(util.format(
							'		<testCase name="%s" duration="%d">',
							espape(test.titleId), test.duration != undefined ? test.duration : 0
						));
						switch(test.state){
							case 'failed':
								append(util.format(
									'			<failure message="%s"><![CDATA[%s]]></failure>',
									espape(test.message), test.stack
								));
								break;
							case 'skipped':	
								append(util.format(
									'			<skipped message="%s"></skipped>', espape(test.title)
								));
								break;
						}
						append('		</testCase>');
				}
			});
			append('	</file>');
		});
		append('</unitTest>');
		
		console.log("Write results to "+mochaFile);
		mkdirp.sync(path.dirname(mochaFile));
		fs.writeFileSync(mochaFile, result, 'utf-8');
	});
};
function append(str) {
	process.stdout.write(str);
	process.stdout.write('\n');
	result += str+"\n";
};


function espape(str){
	str = str || '';
	return str.replace(/&/g, '&amp;')
				.replace(/</g, '&lt;')
				.replace(/>/g, '&gt;')
				.replace(/"/g, '&quot;')
				.replace(/'/g, '&apos;');
}