var net = require('net');
var fs = require('fs');
var http = require('http');

/*
var server = function (socket) {
	socket.name = socket.remoteAddress + ":" + socket.remotePort;

console.log("Welcome " + socket.name);

	socket.on('data', function (data) {
		console.log(data+"");
	});


	socket.on('end', function () {
		console.log(socket.name + " left.(end)");
	});
	socket.on('error', function () {
		console.log(socket.name + " left.(err)\n");
	});
	socket.on('close', function () {
		console.log(socket.name + " left.(close)\n");
	});
};

// Start a TCP Server
net.createServer(server).listen(2000);

// Put a friendly message on the terminal of the server.
console.log("server running at port 5100\n");*/

var frames = [];
var deltas = [];
var BOUNDARY = '----' + Math.random().toString(16).substring(2);
var black = fs.readFileSync('black.jpg');
var qt;
var pull_list = [];
var ON = false;

http.createServer(function (req, res) {
	// set up some routes
	switch(req.url) {
		case '/data':
		case '/':
			if (req.method == 'POST') {
				console.time('data in');
				console.log("[200] " + req.method + " to " + req.url);
				var bufs = [];
				if(qt){
					var now = new Date();
					var delta = now.getTime() - qt.getTime();
					if(delta < 100){
						deltas.push(delta);
						console.log("frame delta: " + delta);
					}else{
						deltas.push(0);
					}
					//delete qt;
					delete now;
					delete delta;
				}else{
					deltas.push(0);
//console.log(req);
				}
				//var file = fs.createWriteStream('test');
				req.on('data', function(chunk) {
					//console.log("Received body data...");
					//console.log(chunk.toString());
					bufs.push(chunk);
					//file.write(chunk);
				});
				req.on('end', function() {
					res.writeHead(200, "OK", {'Content-Type': 'text/html'});
					res.end("ok!");
					//file.end();

					qt = new Date();

					var buf = Buffer.concat(bufs);
					frames.push(buf);
					console.log("Received body end: " + buf.length);
					delete bufs;

					/*fs.writeFile('test-string', buf, function (err) {
						if (err) throw err;
						console.log('It\'s saved!');
					});*/
					console.timeEnd('data in');
				});
			}else{
				console.log("[501] " + req.method + " to " + req.url);
				res.writeHead(501, "Not implemented", {'Content-Type': 'text/html'});
				res.end('<html><head><title>501 - Not implemented</title></head><body><h1>Not implemented!</h1></body></html>');
			}
		break;
		case '/gg.jpg':
		case '/gg.mjpeg':
				res.writeHead(200, {'Content-Type': "multipart/x-mixed-replace; boundary="+BOUNDARY});
				var isBlack = 0;
				/*var push = setInterval(function () {
					//var date = new Date();
					//res.write(date.toString() + '\r\n\r\n', 'ascii');
					if(frames.length){
						res.write(BOUNDARY+'\r\n', 'ascii');
						res.write('Content-Type: image/jpeg\r\n', 'ascii');
						res.write('Content-length: ' + frames[0].length + '\r\n', 'ascii');
						res.write('\r\n', 'ascii');
						res.write(frames[0]);
						//frames.splice(0, 1);
						res.write('\r\n\r\n', 'ascii');
						isBlack = 0;
					}else{
						if(isBlack == 0){
							res.write(BOUNDARY+'\r\n', 'ascii');
							res.write('Content-Type: image/jpeg\r\n', 'ascii');
							res.write('Content-length: ' + black.length + '\r\n', 'ascii');
							res.write('\r\n', 'ascii');
							res.write(black);
							res.write('\r\n\r\n', 'ascii');
							isBlack = 1;
						}
					}
				}, 100);*/
				/*req.on('close', function() {
					clearInterval(push);
					console.log("req end.");
				});*/
				var keep;
				var go = function(){
					if(frames.length){
						res.write(BOUNDARY+'\r\n', 'ascii');
						res.write('Content-Type: image/jpeg\r\n', 'ascii');
						res.write('Content-length: ' + frames[0].length + '\r\n', 'ascii');
						res.write('\r\n', 'ascii');
						res.write(frames[0]);
						res.write('\r\n\r\n', 'ascii');
						//isBlack = 0;
					}else{
						if(isBlack == 0){
							res.write(BOUNDARY+'\r\n', 'ascii');
							res.write('Content-Type: image/jpeg\r\n', 'ascii');
							res.write('Content-length: ' + black.length + '\r\n', 'ascii');
							res.write('\r\n', 'ascii');
							res.write(black);
							res.write('\r\n\r\n', 'ascii');
							isBlack = 1;
						}
					}
					//console.log("push...");
					keep = setTimeout(go, 100);
				}
				go();
				req.on('close', function() {
					clearTimeout(keep);
					delete go;
					delete keep;
					console.log("req end.");
				});

		break;
		case '/show':
			console.log("[200] " + req.method + " to " + req.url);
			res.writeHead(200, "OK", {'Content-Type': 'text/html'});
			res.end('<html><head><title>ipcam</title></head><body><img src="/gg.mjpeg"></body></html>');
		break;
		case '/poll':
			console.log("[200] " + req.method + " to " + req.url);
			res.writeHead(200, "OK", {'Content-Type': 'text/html'});
			var go = function(){
				if(ON){
					res.end('ON');
				}else{
					res.end('OFF');
				}
				console.log("push...");
			}
			pull_list.push({'res': res, 'ref': setTimeout(go, 15000)});
			delete go;
		break;
		case '/set/ON':
			console.log("[200] " + req.method + " to " + req.url);
			res.writeHead(200, "OK", {'Content-Type': 'text/html'});
			res.end('set ON');
			ON = true;
			pull_list.forEach(function(element, index, array) {
				clearTimeout(element.ref);
				element.res.end('ON');
			});
			pull_list = [];
			console.log("set ON...");
		break;
		case '/set/OFF':
			console.log("[200] " + req.method + " to " + req.url);
			res.writeHead(200, "OK", {'Content-Type': 'text/html'});
			res.end('set OFF');
			ON = false;
			pull_list.forEach(function(element, index, array) {
				clearTimeout(element.ref);
				element.res.end('OFF');
			});
			pull_list = [];
			frames = [];
			deltas = [];
			qt = null;
			console.log("set OFF...");
		break;
		default:
			res.writeHead(404, "Not found", {'Content-Type': 'text/html'});
			res.end('<html><head><title>404 - Not found</title></head><body><h1>Not found.</h1></body></html>');
			console.log("[404] " + req.method + " to " + req.url);
	};
}).listen(5900);

/*setInterval(function () {
	if(frames.length>1){
		frames.splice(0, 1);
	}
}, 100);*/
var popout = function(){
	var delta = deltas.pop();
	if(frames.length){
		frames.splice(0, 1);
	}
	if(delta){
		setTimeout(popout, delta);
	}else{
		setTimeout(popout, 10);
	}
	delete delta;
}
popout();


