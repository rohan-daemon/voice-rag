"""Run dependency-free observer regression checks after `mvn package`."""
import pathlib
import subprocess
import tempfile
import zipfile

root = pathlib.Path(__file__).resolve().parent.parent
with tempfile.TemporaryDirectory(prefix="voice-rag-observer-") as temporary:
    with zipfile.ZipFile(root / "target/voice-rag-1.0.0.jar") as jar:
        for name in jar.namelist():
            if name.startswith("BOOT-INF/lib/") and name.endswith(".jar"):
                jar.extract(name, temporary)
    classpath = ":".join([
        str(root / "target/test-classes"),
        str(root / "target/classes"),
        temporary + "/BOOT-INF/lib/*",
    ])
    subprocess.run([
        "java", "-ea", "-cp", classpath,
        "com.voicerag.mcp.ObservedToolCallbackChecks",
    ], check=True)
