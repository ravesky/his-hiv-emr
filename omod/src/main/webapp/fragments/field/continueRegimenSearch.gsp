<div>

<table>
<tbody>
<tr>
<td><input type="button" class="ui-button ui-widget ui-state-default ui-corner-all" id="guide" name="guide" value="Guide" onClick="guideForContinRegimen();" /></td>
<td><input type="button" class="ui-button ui-widget ui-state-default ui-corner-all" id="artRegimen" name="artRegimen" value="Art Regimen" onClick="artForContinRegimen();" /></td>
</tr>
<tr>
<td class="colB" style="text-align:center">Drug</td>
<td class="colC" style="text-align:center">Strength(per tab or ml)</td>
<td class="colD" style="text-align:center">Tablets</td>
<td class="colE" style="text-align:center">Tab/Ml</td>
<td class="colF" style="text-align:center">Frequency</td>
<td class="colG" style="text-align:center">Duration(in days)</td>
<td class="colH" style="text-align:center"></td>
<td class="colI" style="text-align:center"></td>
<td class="colJ" style="text-align:center"></td>
<td class="colK" style="text-align:center"></td>
</tr>
<% drugOrderProcesseds.each { drugOrderProcessed -> %>
<tr>
<td class="colB" style="text-align:center"><input type="text" id="drugKey${count}" name="drugKey${count}" value="${drugOrderProcessed.drugOrder.concept.name}" disabled></td>
<td class="colC" style="text-align:center"><input type="text" id="strength${count}" name="strength${count}" value="${drugOrderProcessed.dose}" disabled></td>
<td class="colD" style="text-align:center"><input type="text" id="noOfTablet${count}" name="noOfTablet${count}" value="${drugOrderProcessed.noOfTablet}" disabled></td>
<td class="colE" style="text-align:center"><input type="text" id="type${count}" name="type${count}" value="${drugOrderProcessed.drugOrder.units}" disabled></td>
<td class="colF" style="text-align:center"><input type="text" id="frequncy${count}" name="frequncy${count}" value="${drugOrderProcessed.drugOrder.frequency}" disabled></td>
<td class="colG" style="text-align:center"><input type="text" id="duration${count}" name="duration${count}"></td>
<td class="colH" style="text-align:center"><input type="button" class="ui-button ui-widget ui-state-default ui-corner-all" size="5" id="info" name="info" value="Info" ng-click="artDrugInfoForRegimenSearch(drugKey);" /></td>
<td class="colI" style="text-align:center"></td>
<td class="colJ" style="text-align:center"></td>
<td class="colK" style="text-align:center"><input type="hidden" id="srNumber${count}" name="srNo" value="${count++}"></td>
</tr>
<% } %>
</tbody>
</table>

</div>

<script type="text/javascript">
var patientId=${patient.patientId};
changeType="Continue";

function guideForContinRegimen(){
jQuery('#guideDiv').empty();
jQuery('#drugInfoDiv').empty();
var age=${patient.age};
if(age>14){
var htmlText =  "<img src='${ ui.resourceLink('kenyaui', 'images/glyphs/flow_chart_adult.jpg') }' />"
var newElement = document.createElement('div');
newElement.setAttribute("id", "guideDivAdult"); 
newElement.innerHTML = htmlText;
var fieldsArea = document.getElementById('guideDiv');
fieldsArea.appendChild(newElement);
var url = "#TB_inline?height=750&width=1150&inlineId=guideDivAdult";
tb_show("Guide",url,false);
}
else{
var htmlText =  "<img src='${ ui.resourceLink('kenyaui', 'images/glyphs/flow_chart_child.jpg') }' />"
var newElement = document.createElement('div');
newElement.setAttribute("id", "guideDivChild"); 
newElement.innerHTML = htmlText;
var fieldsArea = document.getElementById('guideDiv');
fieldsArea.appendChild(newElement);
var url = "#TB_inline?height=750&width=1350&inlineId=guideDivChild";
tb_show("Guide",url,false);
}
}

function artForContinRegimen(){
jQuery('#artRegimenDiv').empty();
//jQuery('#drugInfoDiv').empty();
var age=${patient.age};
if(age>14){
var htmlText =  "<img src='${ ui.resourceLink('kenyaui', 'images/glyphs/artRegimen_adultImage.jpg') }' />"
var newElement = document.createElement('div');
newElement.setAttribute("id", "artRegimenDivAdult"); 
newElement.innerHTML = htmlText;
var fieldsArea = document.getElementById('artRegimenDiv');
fieldsArea.appendChild(newElement);
var url = "#TB_inline?height=700&width=1450&inlineId=artRegimenDivAdult";
tb_show("Guide",url,false);
}
else{
var htmlText =  "<img src='${ ui.resourceLink('kenyaui', 'images/glyphs/artRegimen_childImage.jpg') }' />"
var newElement = document.createElement('div');
newElement.setAttribute("id", "artRegimenDivChild"); 
newElement.innerHTML = htmlText;
var fieldsArea = document.getElementById('artRegimenDiv');
fieldsArea.appendChild(newElement);
var url = "#TB_inline?height=1000&width=1450&inlineId=artRegimenDivChild";
tb_show("Guide",url,false);
}
}
</script>

<style type="text/css">
  table { width: 100%; }
  td.colB { width: 10%; }
  td.colC { width: 10%; }
  td.colD { width: 10%; }
  td.colE { width: 10%; }
  td.colF { width: 10%; }
  td.colG { width: 10%; }
  td.colH { width: 5%; }
  td.colI { width: 5%; }
  td.colJ { width: 5%; }
  td.colK { width: 5%; }
</style>