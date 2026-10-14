import { spawn } from 'node:child_process';

// Ignore inherited deployment credentials and JVM injection options.
const env = Object.fromEntries(Object.entries(process.env).filter(([key]) =>
  !/^(SPRING_|APP_|CRAWLER_|POSTGRES_|JAVA_TOOL_OPTIONS$|JDK_JAVA_OPTIONS$|GRADLE_OPTS$)/.test(key)));
const child = spawn('./gradlew', ['--no-daemon', '--console=plain', ':web-app:uiServer'], { env, stdio: 'inherit', detached: true });
let stopping = false;
function stop() {
  if (stopping) return;
  stopping = true;
  process.kill(-child.pid, 'SIGTERM');
  const force = setTimeout(() => { try { process.kill(-child.pid, 'SIGKILL'); } catch {} }, 5000);
  force.unref();
}
process.on('SIGTERM', stop);
process.on('SIGINT', stop);
child.on('exit', code => process.exit(code ?? (stopping ? 0 : 1)));
