import os

TEST_SERVER_DIR = 'test_server'
JAR_NAME = 'AikarMobFix.jar'

if not os.path.exists(TEST_SERVER_DIR):
    os.mkdir(TEST_SERVER_DIR)

os.chdir(TEST_SERVER_DIR)

if not os.path.exists('craftbukkit.jar'):
    os.system('curl -L -o craftbukkit.jar http://dl.bukkit.org/latest-rb/craftbukkit.jar')

if not os.path.exists('plugins'):
    os.mkdir('plugins')
    os.symlink(os.path.join('..', '..', 'dist', JAR_NAME), os.path.join('plugins', JAR_NAME))

if not os.path.exists('run_server.sh'):
    open('run_server.sh', 'w').write("""\
#!/bin/bash
cd $(dirname "$0")
java -Xmx1024M -Xms1024M -jar craftbukkit.jar""")
