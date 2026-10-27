"""Gradle launcher for cloud HTTPS proxies. Does not disable TLS verification."""
import os, subprocess, sys, urllib.parse, shlex
from pathlib import Path
root = Path(__file__).resolve().parents[1]
args = [str(root / 'gradlew'), *sys.argv[1:]]
proxy = urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY', ''))
options = []
if proxy.hostname:
    if proxy.username or proxy.password:
        raise SystemExit('Use a supported credential-free platform proxy, not credentials in JVM arguments.')
    for protocol in ('http', 'https'):
        options += ['-D' + protocol + '.proxyHost=' + proxy.hostname,
                 '-D' + protocol + '.proxyPort=' + str(proxy.port or 80)]
if Path('/etc/ssl/certs/java/cacerts').exists():
    options += ['-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']
env = os.environ.copy()
env['JAVA_OPTS'] = env.get('JAVA_OPTS', '') + ' ' + shlex.join(options)
raise SystemExit(subprocess.call(args + options, cwd=root, env=env))
