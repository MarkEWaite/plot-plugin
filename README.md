# Plot plugin

[![codecov](https://codecov.io/gh/jenkinsci/plot-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/jenkinsci/plot-plugin)
[![Jenkins Plugins](https://img.shields.io/jenkins/plugin/v/plot)](https://plugins.jenkins.io/plot)
[![GitHub Release](https://img.shields.io/badge/dynamic/json?color=blue&label=changelog&query=$.tag_name&url=https://api.github.com/repos/jenkinsci/plot-plugin/releases/latest)](https://github.com/jenkinsci/plot-plugin/releases/latest)
[![Jenkins Plugin installs](https://img.shields.io/jenkins/plugin/i/plot?color=blue)](https://plugins.jenkins.io/plot/)

## Description

This plugin provides generic plotting (or graphing) capabilities in Jenkins.

This plugin will **plot** one or more **single values variations across builds** in one or more plots.
Plots for a particular job (or project) are configured in the job configuration screen,
where each field has additional **help information**. Each plot can have one or more lines (called data series).
After each build completes, the latest values are pulled
from **Java properties file(s), CSV file(s), or XML file(s)** via an XPath (which you should have generated during the build) somewhere below your workspace.
Data for each plot is stored in a CSV file within the job's root project directory.

It can generate various kind of plots, including `Area`, `Bar`, `Line`, `Stacked Bar`, `Waterfall`, etc.

Here is an example of the plots generated by this plugin:

![plot-example](./screenshots/plot-example.png)

## Configuration

### Currently supported parameters

The currently supported parameters are:

- **`width (int, default: 750)`** The width of the plot in pixels.
- **`height (int, default: 450)`** The height of the plot in pixels.
- **`rightBuildNum (int, default: 2^38 - 1)`** The right-most build number on the plot.
- **`hasLegend (boolean, default: true)`** Whether or not the plot has a legend.
- **`urlNumBuilds (string, default: 2^38 - 1)`** Number of builds back to show on this plot from URL.
- **`urlTitle (string, default: "")`** Title of plot from URL.
- **`urlStyle (string, default: "")`** Style of plot from URL.
- **`urlUseDescr (boolean, default: false)`** Use description flag from URL.
- **`title (string, default: "")`** Title of plot.
- **`yaxis (string, default: "")`** Y-axis label.
- **`series (list)`** List of data series.
- **`group (string)`** Group name that this plot belongs to.
- **`numBuilds (string, default:"")`**
Number of builds back to show on this plot. An empty string means all builds. Must not be "0".
- **`csvFileName (string, default: "$ROOT_DIR/plot-XXXX.csv")`**
The name of the CSV file that persists the plots data. The CSV file is stored in the projects root directory.
This is different from the source CSV that can be used as a source for the plot.
- **`csvLastModification (long, default: "last modified date")`** The date of the last change to the CSV file.
- **`style (string, default: "line")`** Style of plot: line, line3d, stackedArea, stackedBar, etc.
- **`useDescr (boolean, default: false)`** Whether or not to use build descriptions as X-axis labels.
- **`keepRecords (boolean, default: false)`** Keep records for builds that were deleted.
- **`exclZero (boolean, default: false)`** Whether or not to exclude zero as default Y-axis value.
- **`logarithmic (boolean, default: false)`** Use a logarithmic Y-axis.
- **`yaxisMinimum (string, default: "")`** Minimum y-axis value.
- **`yaxisMaximum (string, default: "")`** Maximum y-axis value.

For a full list of parameters the best place to view is [Plot.java](./src/main/java/hudson/plugins/plot/Plot.java) class.

#### Pipeline job configuration

You can generate the required [Scripted Pipeline](https://jenkins.io/doc/book/pipeline/syntax/#scripted-pipeline)
syntax via the [Snippet Generator](https://jenkins.io/blog/2016/05/31/pipeline-snippetizer/) by choosing the `"plot: Plot build data"` step.

Below you can find sample configuration which is auto-generated using [Snippet Generator](https://jenkins.io/blog/2016/05/31/pipeline-snippetizer/).

```groovy
plot csvFileName: 'plot-8e54e334-ab7b-4c9f-94f7-b9d8965723df.csv',
        csvSeries: [[
                            file: 'data.csv',
                            exclusionValues: '',
                            displayTableFlag: false,
                            inclusionFlag: 'OFF',
                            url: '']],
        group: 'Plot Group',
        title: 'Plot Title',
        style: 'line',
        exclZero: false,
        keepRecords: false,
        logarithmic: false,
        numBuilds: '',
        useDescr: false,
        yaxis: '',
        yaxisMaximum: '',
        yaxisMinimum: ''
```

- **csvFileName** - autogenerated value, but you might want to change it to something more descriptive for your case.
- **file** - source file for plot generation (relative to workspace)

## JIRA issues

If you have any proposals/bug reports, please create an issue on Jenkins [JIRA](https://www.jenkins.io/participate/report-issue/redirect/#15564).

## Changelog

Release notes for current releases are in the [Github releases page](https://github.com/jenkinsci/plot-plugin/releases).
Releases notes prior to June 2019 are available in the [historical archive](https://github.com/jenkinsci/plot-plugin/tree/plot-2.2.0?tab=readme-ov-file#old-release-notes).
