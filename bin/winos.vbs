On error resume next  

'local pc 
strComputer = "."  

'connect to local pc
Set objWMIService = GetObject("winmgmts:\\" & strComputer & "\root\cimv2")  

Set memItems = objWMIService.ExecQuery("Select * from Win32_PerfRawData_PerfOS_Memory",,48)  

For Each objItem in memItems  
    WScript.Echo(objItem.AvailableMBytes)
Next  

 
Set cpuItems = objWMIService.ExecQuery("Select * from Win32_Processor",,48)  

For Each objItem in cpuItems  
		WScript.Echo(100-objItem.LoadPercentage)
Next  
