if [ `uname -s|grep -i linux` ]; then
        vmstat 1 1|grep -v cpu|grep -v id|awk '{print $15}'
fi

if [ `uname -s|grep -i aix` ]; then
        vmstat 1 1|grep -v cpu|grep -v id|awk '{print $16}'|tail -1
fi

if [ `uname -s|grep -i hp` ]; then
        vmstat 1 1|grep -v cpu|grep -v id|awk '{print $18}'
fi