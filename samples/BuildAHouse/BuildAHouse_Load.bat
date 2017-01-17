echo off
call BuildAHouse_Common.bat
set ASSIGNEE=Duc,Another
java %JAVA_FLAGS% -jar ..\..\release\RedmineToGanttProj.jar load
pause

