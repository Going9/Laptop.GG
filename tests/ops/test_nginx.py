from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
import shutil
import socket
import subprocess
import tempfile
import threading
import time
import unittest
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[2]


class Backend(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(204)
        self.send_header('Echo-For', self.headers.get('X-Forwarded-For', 'none'))
        self.send_header('Echo-Forwarded', self.headers.get('Forwarded', 'none'))
        self.end_headers()

    do_POST = do_PUT = do_DELETE = do_GET

    def log_message(self, *args):
        pass


@unittest.skipUnless(shutil.which('nginx'), 'nginx executable is required')
class NginxTest(unittest.TestCase):
    def test_headers_body_limit_proxy_sanitization_and_comment_throttling(self):
        backend = HTTPServer(('127.0.0.1', 0), Backend)
        thread = threading.Thread(target=backend.serve_forever, daemon=True)
        thread.start()
        try:
            with tempfile.TemporaryDirectory() as temporary:
                folder = Path(temporary)
                with socket.socket() as probe:
                    probe.bind(('127.0.0.1', 0))
                    port = probe.getsockname()[1]
                proxy = (ROOT / 'ops/nginx/laptopgg-proxy.conf').read_text().replace('127.0.0.1:8080', f'127.0.0.1:{backend.server_port}')
                (folder / 'proxy.conf').write_text(proxy)
                config = folder / 'nginx.conf'
                config.write_text(f'''
pid {folder}/nginx.pid;
error_log stderr warn;
events {{ worker_connections 32; }}
http {{
    access_log off;
    client_body_temp_path {folder}/body;
    proxy_temp_path {folder}/proxy;
    include {ROOT}/ops/nginx/laptopgg-http.conf;
    server {{
        listen 127.0.0.1:{port};
        include {ROOT}/ops/nginx/laptopgg-headers.conf;
        location / {{ include {folder}/proxy.conf; }}
    }}
}}
''')
                process = subprocess.Popen(['nginx', '-p', str(folder), '-c', str(config), '-g', 'daemon off; master_process off;'], stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)

                def request(path='/', body=None, headers=None):
                    try:
                        return urllib.request.urlopen(urllib.request.Request(f'http://127.0.0.1:{port}' + path, data=body, headers=headers or {}), timeout=2)
                    except urllib.error.HTTPError as error:
                        return error

                try:
                    deadline = time.monotonic() + 5
                    while True:
                        try:
                            response = request()
                            break
                        except urllib.error.URLError:
                            if process.poll() is not None or time.monotonic() >= deadline:
                                process.terminate()
                                _, errors = process.communicate(timeout=5)
                                self.fail('nginx did not start: ' + errors.decode())
                    self.assertEqual(response.status, 204)
                    self.assertEqual(response.headers['X-Content-Type-Options'], 'nosniff')
                    self.assertIn("frame-ancestors 'none'", response.headers['Content-Security-Policy'])
                    self.assertIn("script-src 'self' https://static.cloudflareinsights.com;", response.headers['Content-Security-Policy'])
                    self.assertIn("connect-src 'self' https://cloudflareinsights.com;", response.headers['Content-Security-Policy'])
                    forged = request(headers={'CF-Connecting-IP': '198.51.100.12', 'X-Forwarded-For': '198.51.100.13', 'Forwarded': 'for=198.51.100.14;host=evil.example'})
                    self.assertEqual(forged.headers['Echo-For'], '127.0.0.1')
                    self.assertEqual(forged.headers['Echo-Forwarded'], 'none')
                    self.assertEqual(request('/comments', b'x' * 17000).status, 413)
                    statuses = [request('/api/comments', b'{}', {'CF-Connecting-IP': f'198.51.100.{i}'}).status for i in range(20)]
                    self.assertIn(204, statuses)
                    self.assertIn(429, statuses)
                    self.assertTrue(all(request('/api/comments').status == 204 for _ in range(15)))
                finally:
                    process.terminate()
                    process.communicate(timeout=5)
        finally:
            backend.shutdown()
            backend.server_close()
            thread.join(timeout=5)


if __name__ == '__main__':
    unittest.main()
