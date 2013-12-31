if [ `uname -s|grep -i linux` ]; then
        free memory|grep Mem|awk '{printf "%4.2fM", $4 / 1024}'
fi

if [ `uname -s|grep -i aix` ]; then
        vmstat |grep -v -E "cpu|id|-"|awk '{printf "%4.2fM", $4 * 4 / 1024}'
fi

if [ `uname -s|grep -i hp` ]; then
        vmstat |grep -v -E "cpu|id|-"|awk '{printf "%4.2fM", $4 * 4 / 1024}'
fi