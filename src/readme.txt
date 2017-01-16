****************************************************************************
GanttProjectAPI
ObjectCode
https://sourceforge.net/projects/ganttprojectapi/
****************************************************************************

I. Description

  This package is to get and set some Data from GanttProject-XML-Files
          
  It supports the following features:

    o  Read data from GanttDiagram-Charts
    o  Write data into GanttDiagram-Charts
    o  Modify data from GanttDiagram-Charts
    o  Test itself with JUnit


    o  not finished yet

    
II. File List

  Java-Classes:
      o   GanttDiagram.java					- Wrapper-class for GanttDiagramms
      o   Task.java							- Handles GanttDiagram-Tasks
      o   Resource.java						- Handles GanttDiagram-Resources
      o   Role.java							- Handles GanttDiagram-Roles
      o   TaskProperty.java					- Handles GanttDiagram-Task-Properties
      o   ResourceAllocation.java			- Handles GanttDiagram-Resource-Allocations
      o   Depend.java						- Handles GanttDiagram-Task-Dependencies
      o   XMLHelper.java					- Read XMLFile and parse it into dom
  
  For testing:    
      o   Test1.java						- Read data from gantt-xml-file - for testing
      o   Test2.java						- Read and modify data from gantt-xml-file - for testing
      o   Test3.java						- Read and delete data from gantt-xml-file - for testing
      o   GanttProjectAPIJUnitReadTest.java	- Some JUnit-Tests
      o   Demo1.gan							- GanttChart from GanttProject for Test1.java and JUnitTest
      o   Demo1b.gan						- GanttChart from GanttProject for Test2.java
      o   Demo1b_orig.gan					- original file from Demo1b.gan
      o   Demo2.gan							- GanttChart from GanttProject for some other tests
      o   Demo3.gan							- GanttChart from GanttProject for the delete tests
      o   Demo3_orig.gan					- original file from Demo3.gan
  
  libraries:
  	  o   junit-4.8.2.jar
  	  o   log4j.jar
  	  o   org.hamcrest.core_1.1.0.v20090501071000.jar
  	  o   xml-apis.jar
  
  Other:
      o   readme.txt  						
      o   index.html						      

  Executable files:
      o   Test1.sh 							- Use this to test Test1.java without an IDE under Unix-systems (i.e. sh Test1.sh)
      o	  Test1.bat							- Use this to test Test1.java without an IDE under Windows-systems
      o   Test2.sh							- Use this to test Test2.java without an IDE under Unix-systems (i.e. sh Test2.sh)
      o   Test2.bat							- Use this to test Test2.java without an IDE under Windows-systems
      o   Test3.sh							- Use this to test Test3.java without an IDE under Unix-systems (i.e. sh Test2.sh)
      o   Test3.bat							- Use this to test Test3.java without an IDE under Windows-systems
      o   JUnittest1.sh						- Use this to test GanttDiagram.java with JUnit without an IDE under Unix-systems (i.e. sh Test1.sh)
	  o   JUnittest1.bat					- Use this to test GanttDiagram.java with JUnit without an IDE under Windows-systems
      o   JUnittest2.sh						- Use this to test GanttDiagram.java with JUnit (write-tests) without an IDE under Unix-systems (i.e. sh Test1.sh)
	  o   JUnittest2.bat					- Use this to test GanttDiagram.java with JUnit (write-tests) without an IDE under Windows-systems

III. Requirements

    1. check out all Files

    2.  To test, make sure the absolute path for the test-gantt-charts (Demo1.gan, Demo1b.gan or Demo2.gan) in the TestX.java or/and GanttProjectAPIJUnitTest.java is correct
			
			i.e. on Windows		:	C:\\Projects\\GanttProjectAPI\\data\\Demo1.gan
			
			i.e. on Linux/unix	:	/home/user/workspace/GanttProjectAPI/data/Demo1b.gan

        Would be easier to use Eclipse or another IDE, but not necessary
    


IV. How to Use

	o Test the read-function from GanttProjectAPI:

      o	  For a general test, use one of the executable files (Win: Test1.bat or Unix: Test1.sh)
      o   normally you have to change some paths in the .sh or .bat files
      		o Update CP="CLASSPATH"
      		o Update JAVAHOME="JAVAPATH"
      o   It use the Test1.java and Demo1.gan
	  o   If you want you can edit the Test1.java for your own tests
	  
	o Test the write-function from GanttProjectAPI
	 
	  o	  For a general test, use one of the executable files (Win: Test2.bat or Unix: Test2.sh)
      o   normally you have to change some paths in the .sh or .bat files
	  o   It use the Test2.java and Demo1b.gan
	  o   If you want you can edit the Test2.java for your own tests
	  o   After the first launch the Demo1b.gen witch is used by Test2.java is overwritten with the new (changed) data
	  	  you have to edit Test2.java to see effects of a next launch
	  
	  
	o Use the API (first steps, examples):
	  
	  o	read out data from *.gan:
	  	
	  	String ganttDiagramFile = "/home/objectcode/workspace/GanttProjectAPI/data/Demo1.gan";		//Use this or an equal path for Unix-systems
	  	or
	  	String ganttDiagramFile = "C:\\Projects\\GanttProjectAPI\\data\\Demo1.gan";				//Use this or an equal path for Windows-systems
	  	
	  	GanttDiagram ganttDiagram = new GanttDiagram(ganttDiagramFile);
		ganttDiagram.loadGanttDiagram();
		
		log("Project-Name: "+ganttDiagram.getName());
		log("Task with Id=4: " + ganttDiagram.getTaskById("4").toString());
		log("Name from Task with Id=5: " + ganttDiagram.getTaskName("5")
		log("Funkion of Resource with Id=3: " + ganttDiagram.getResourceFunction("3"));
		log("Taskproperty tpd0 Name: " + ganttDiagram.getTaskPropertyName("tpd0")); 
	  	...
	  	
	  	
	  o	edit data from *.gan
	  	
	  	...
	  	ganttDiagram.modifyDiagram_setProjectName("Demo1b new");
    	ganttDiagram.modifyDiagram_setVersion("2.1");
	   	ganttDiagram.modifyDiagram_addTask("101", "3", "7", "NewTask", 0, 1, false, "1", new java.util.Date(), "created by Test2.java");
   		ganttDiagram.modifyDiagram_setTaskName("1", "new TaskName");
    	ganttDiagram.modifyDiagram_setTaskMeeting("1", "true");
    	...
    	ganttDiagram.modifyDiagram_deleteTask("1");
    	...
    	
    	
V. History:

	First standalone test for reading and JUnittest release: 2010/06/15
	First standalone test for writing release : 2010/06/16
	First standalone test for delete release : 2010/06/21
    
VI. Compatibility

	Compatible with Java-JDK 1.6
    
VII. Known Issues

    None

VIII. Additional Resources

      o   http://www.ganttproject.biz/

      o   http://www.junit.org/
      o   http://www.eclipse.org
      o   http://www.sourceforge.net/projects/eclipsessgen