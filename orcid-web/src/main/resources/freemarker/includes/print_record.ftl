<#--

    =============================================================================

    ORCID (R) Open Source
    http://orcid.org

    Copyright (c) 2012-2014 ORCID, Inc.
    Licensed under an MIT-Style License (MIT)
    http://orcid.org/open-source-license

    This copyright and license information (including a link to the full license)
    shall be included in its entirety in all copies or substantial portion of
    the software.

    =============================================================================

-->
    
    <!--
	<div class="print-orcid-record">

		<a id="printRecord" onclick="printPublicRecord('https://localhost:8443/orcid-web/${(effectiveUserOrcid)!}')"><span class="glyphicon glyphicon-print"></span> Print your ORCID record</a>		
        <div class="popover-help-container">
            <a href="javascript:void(0);"><i class="glyphicon glyphicon-question-sign"></i></a>
            <div id="print-help" class="popover bottom">
                <div class="arrow"></div>
                <div class="popover-content">
                    <p>Print the public view of your ORCID record</p>
                </div>
            </div>
        </div>
    </div>
    -->
    <div class="print-orcid-record">
        <a onclick="javascript:testJsPdf();"><span class="glyphicon glyphicon-floppy-save"></span></span> Save PDF</a>
        <div class="popover-help-container">
            <a href="javascript:void(0);"><i class="glyphicon glyphicon-question-sign"></i></a>
            <div id="save-pdf-help" class="popover bottom">
                <div class="arrow"></div>
                <div class="popover-content">
                    <p>Save and download the public view of your ORCID record as a PDF file</p>
                    <#--<p><@orcid.msg 'workspace.qrcode.help'/></p>-->
                </div>
            </div>
        </div>
	</div>


