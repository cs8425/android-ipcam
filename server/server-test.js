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
net.createServer(server).listen(5100);

// Put a friendly message on the terminal of the server.
console.log("server running at port 5100\n");*/
var frames = [];
var BOUNDARY = '----' + Math.random().toString(16).substring(2);

http.createServer(function (req, res) {
	// set up some routes
	switch(req.url) {
		case '/':
			if (req.method == 'POST') {
				console.log("[200] " + req.method + " to " + req.url);
				var bufs = [];
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
					console.log("Received body end");
					//file.end();
					var buf = Buffer.concat(bufs);
					frames.push(buf);
					delete bufs;
					/*fs.writeFile('test-string', buf, function (err) {
						if (err) throw err;
						console.log('It\'s saved!');
					});*/
				});
			}else{
				console.log("[501] " + req.method + " to " + req.url);
				res.writeHead(501, "Not implemented", {'Content-Type': 'text/html'});
				res.end('<html><head><title>501 - Not implemented</title></head><body><h1>Not implemented!</h1></body></html>');
			}
		break;
		case '/gg.jpg':
				res.writeHead(200, {'Content-Type': "multipart/x-mixed-replace; boundary="+BOUNDARY});
				setInterval(function () {
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
					}/*else{
						res.write(BOUNDARY+'\r\n', 'ascii');
						res.write('Content-Type: image/jpeg\r\n', 'ascii');
						res.write('Content-length: ' + frames[0].length + '\r\n', 'ascii');
						res.write('\r\n', 'ascii');
						res.write(frames[0]);
						res.write('\r\n\r\n', 'ascii');
					}*/
				}, 10);
		break;
		default:
			res.writeHead(404, "Not found", {'Content-Type': 'text/html'});
			res.end('<html><head><title>404 - Not found</title></head><body><h1>Not found.</h1></body></html>');
			console.log("[404] " + req.method + " to " + req.url);
	};
}).listen(5100);

setInterval(function () {
	if(frames.length){
		frames.splice(0, 1);
	}
}, 40);


