<%
	ui.decorateWith("kenyaui", "panel", [ heading: "NAP MONTHLY REPORT(Child)" ])

%>

<form id="napmonthlyreportchildform">

	<label class="ke-field-label">Request Report:</label>
	<span class="ke-field-content">
	Start Date
	${ ui.includeFragment("kenyaui", "widget/field", [ id: "start-date", object: command, property: "startDate" ]) }
	
	<br/>
	<br/>
	End Date&nbsp;&nbsp;
	${ ui.includeFragment("kenyaui", "widget/field", [ id: "end-date", object: command, property: "endDate" ]) }
	</span>
	
	<input type="button" value="View" onclick="viewReport();"/>
	<input type="button" value="Export" onclick="exportYearlyReportToExcel('exportYearlyReport');"/>
	<input type="button" value="Cancel" onclick="ke_cancel();"/>
</form>


<script type="text/javascript">
//View Report
function viewReport() {
var startDate=jQuery('#start-date').val();
var endDate=jQuery('#end-date').val();
var ageCategory="<=14";
jQuery('#viewReport').empty();

jQuery.ajax({
				type : "GET",
				url : getContextPath() + "/kenyaemr/reports/getNapMonthlyReportForChild.page",
				data : ({
					startDate:startDate,
					endDate:endDate,
					ageCategory:ageCategory
				}),
				success : function(data) {
				jQuery("#viewReport").html(data);	
				}
  });
}

//Excel export
var exportYearlyReportToExcel = (function() {
			
var year=jQuery('#year').val();
year=2017;
jQuery('#exportYearlyReport').empty();
jQuery.ajax({
				type : "GET",
				url : getContextPath() + "/kenyaemr/reports/getYearlyReport.page",
				data : ({
					year:year
				}),
				success : function(data) {
				jQuery("#exportYearlyReport").html(data);	
				}
         });
         
		var uri = 'data:application/vnd.ms-excel;base64,'
		, template = '<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40"><head><!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>{worksheet}</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]--></head><body><table >{table}</table></body></html>'
		, base64 = function(s) { return window.btoa(unescape(encodeURIComponent(s))) }
		, format = function(s, c) { return s.replace(/{(\\w+)}/g, function(m, p) { return c[p]; }) }
		return function(table, name) {
		if (!table.nodeType) table = document.getElementById(table)
		var ctx = {worksheet: name || 'White Card', table: table.innerHTML}
		
		var link = document.createElement("a");
		link.href = uri + base64(format(template, ctx));

		link.style = 'visibility:hidden';
		link.download ='${ currDate } - yearly report.xls';

		document.body.appendChild(link);
		link.click();
		
		}
	})()

// get context path in order to build controller url
	function getContextPath() {
		pn = location.pathname;
		len = pn.indexOf("/", 1);
		cp = pn.substring(0, len);
		return cp;
	}

function ke_cancel() {
			ui.navigate('kenyaemr', 'reports/reportsHome');
}
</script>

<div class="ke-page-content" id="exportYearlyReport" hidden="hidden">

</div>