var assert = Packages.org.junit.Assert;
var jsAssert = {};
var TestCase = Packages.uk.co.benjiweber.junitjs.TestCase;
var ScriptHelper = Java.type('nxt.ScriptHelper');

function objectEquals( x, y ) {
  if ( x === y ) return true;
  if ( ! ( x instanceof Object ) || ! ( y instanceof Object ) ) return false;
  if ( x.constructor !== y.constructor ) return false;
  for ( var p in x ) {
    if ( ! x.hasOwnProperty( p ) ) continue;
    if ( ! y.hasOwnProperty( p ) ) return false;
    if ( x[ p ] === y[ p ] ) continue;
    if ( typeof( x[ p ] ) !== "object" ) return false;
    if ( ! Object.equals( x[ p ],  y[ p ] ) ) return false;
  }
  for ( p in y ) {
    if ( y.hasOwnProperty( p ) && ! x.hasOwnProperty( p ) ) return false;
  }
  return true;
}

jsAssert.assertIntegerEquals = function(a, b) {
	if (a === b) return;	
	throw new Packages.org.junit.ComparisonFailure("Expected <" + a + "> but was <" + b + ">", a, b);
}
jsAssert.assertObjectEquals = function(a, b) {
  if (objectEquals(a,b)) return;  
  throw new Packages.org.junit.ComparisonFailure("Expected <" + JSON.stringify(a) + "> but was <" + JSON.stringify(b) + ">", a, b);
}
jsAssert.assertEquals = function (a,b) {
  if (a==b) return;  
  throw new Packages.org.junit.ComparisonFailure("Expected <" + JSON.stringify(a) + "> but was <" + JSON.stringify(b) + ">", a, b);
}

var nashornDetector = {
	__noSuchMethod__:  function(name, arg0, arg1) {
		return typeof arg1 != "undefined";
	}
}

var isRhino = function() {
	return !nashornDetector.detect('one','two');
}

var console = {
	log: function(text) {
		print(text + (isRhino() ? "\n" : ""));
	}
}

var newStub = function() {
	return 	{
		called: [],
		__noSuchMethod__:  function(name, arg0, arg1, arg2, arg3, arg4, arg5) {
			var desc = {
				name: name,
				args: []
			};
			var rhino = arg0.length && typeof arg1 == "undefined";
			
			var args = rhino ? arg0 : arguments;
			for (var i = rhino ? 0 : 1; i < args.length; i++){
				if (typeof args[i] == "undefined") continue;
				desc.args.push(args[i]);
			}
			this.called.push(desc);
		},
		
		assertCalled: function(description) {
			
			var fnDescToString = function(desc) {
				return desc.name + "("+ desc.args.join(",") +")";
			};
			
			if (this.called.length < 1) assert.fail('No functions called, expected: ' + fnDescToString(description));

			for (var i = 0; i < this.called.length; i++) {
				var fn = this.called[i];
				if (fn.name == description.name) {
					if (description.args.length != fn.args.length) continue;
					
					for (var j = 0; j < description.args.length; j++) {
						if (fn.args[j] == description.args[j]) return;
					}
				}
			}
			
			assert.fail('No matching functions called. expected: ' + 
					'<' + fnDescToString(description) + ")>" +
					' but had ' +
					'<' + this.called.map(fnDescToString).join("|") + '>'
			);
		}
	};
};

function createTestFunction(inner_test, before, after) {
  return function () {
    ScriptHelper.rollback(50);
    ScriptHelper.truncate("unconfirmed_transaction;");
    Nxt.generateBlock();
    Nxt.verbose = false;
    var global = {};
    try {
      if (typeof before == 'function') before.call(global);
      inner_test.call(global);
    } finally {
      if (typeof after == 'function') after.call(global);
    }
  }
}

var tests = function(testObject) {
	var testCases = new java.util.ArrayList();
	var before = testObject['Before'];
    delete testObject['Before'];

	var after = testObject['After'];
    delete testObject['After'];

	for (var name in testObject) {
	  if (testObject.hasOwnProperty(name)) {
	    testCases.add(new TestCase(name, createTestFunction(testObject[name], before, after)));
	  }
	}
	testCases.add(new TestCase('print results', function () {
	   
	 
	    
	}));
	return testCases;
};
