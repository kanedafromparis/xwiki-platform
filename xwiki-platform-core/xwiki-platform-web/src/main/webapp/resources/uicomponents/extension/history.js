/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
require(['jquery', 'xwiki-events-bridge'], function($) {
  //
  // Extension History Sources
  //

  $('.extension-history-sources-header').click(function() {
    $(this).closest('.extension-history-sources-selector').toggleClass('opened');
  });

  $('.extension-history-sources-selector').removeClass('opened');

  var onDeleteHistorySource = function(event) {
    event.preventDefault();
    var deleteLink = $(this);
    if (!window.confirm(deleteLink.attr('data-confirmation'))) {
      return;
    }
    deleteLink.addClass('loading').children().hide();
    // Reduce the cost of the request by disabling the redirect.
    var deleteURL = deleteLink.attr('href').replace(/xredirect=/, 'xfoo=');
    $.get(deleteURL).done(function() {
      var source = deleteLink.closest('.extension-history-source');
      if (source.hasClass('selected')) {
        // Select the 'local history' source.
        var localHistorySource = source.parent().children().first();
        onSelectHistorySource.call(localHistorySource.children('.extension-history-source-name'));
      }
      source.remove();
    }).fail(function() {
      deleteLink.removeClass('loading').children().show();
    });
  };

  var onSelectHistorySource = function(event) {
    event && event.preventDefault();
    var source = $(this).closest('.extension-history-source');
    if (source.hasClass('selected')) {
      // Source already selected.
      return;
    }
    // Clear the selection.
    source.parent().children('.selected').removeClass('selected');
    // Select the new source.
    source.addClass('selected');
    // Close the source selector and update the source list hint.
    var sourceSelector = source.closest('.extension-history-sources-selector');
    sourceSelector.removeClass('opened').find('.extension-history-sources-header em').text($(this).text());
    // Update the list of history records.
    var recordsForm = sourceSelector.next('.extension-history-records-form');
    // Hide the history records from the currently selected source.
    recordsForm.find('.extension-history-record').hide();
    // Also hide the records form action buttons.
    recordsForm.find('.extension-history-actions').hide();
    // Add a loading history record.
    recordsForm.children('.extension-history-records')
      .append('<li class="extension-history-record loading"><span style="visibility:hidden">Loading</span></li>');
    // Request the history records.
    $.get(source.attr('data-recordsURL')).done(function(html) {
      recordsForm.replaceWith(html);
      recordsForm = sourceSelector.next('.extension-history-records-form');
      recordsForm.size() > 0 && $(document).trigger('xwiki:dom:updated', {'elements': recordsForm.toArray()});
    }).fail(function() {
      // TODO
    });
  };

  var enhanceHistorySources = function(container) {
    container.find('.extension-history-source a.deleteLink').click(onDeleteHistorySource);
    container.find('a.extension-history-source-name').click(onSelectHistorySource);
  };
  enhanceHistorySources($('.extension-history-sources'));

  var onUploadMessage = function(event, message) {
    if (message.type === 'done') {
      var sourcesContainer = $(event.target).closest('.extension-history-sources-body');
      enhanceHistorySources(sourcesContainer);
      // Select the uploaded source.
      var sourceFileName = message.parameters.name;
      var source = sourcesContainer.find('.extension-history-source').filter(function() {
        return $(this).attr('data-fileName') === sourceFileName;
      }).first();
      onSelectHistorySource.call(source.find('.extension-history-source-name'));
    }
  };

  if (typeof(XWiki.FileUploader) != 'undefined') {
    $('.extension-history-source-upload input[type="file"]').each(function() {
      $(this).on('xwiki:html5upload:message', onUploadMessage);
      new XWiki.FileUploader(this, {
        'maxFilesize': 1000000,
        'fileFilter': /application\/xml|text\/xml/i,
        'progressAutohide': true,
        'responseContainer' : $('.extension-history-sources')[0]
      });
    });
  }

  //
  // Extension History Records
  //

  var loadMoreHistoryRecords = function(event) {
    event.preventDefault();
    var moreLink = $(event.target).css('visibility', 'hidden');
    var loadingRecord = moreLink.closest('.extension-history-record').addClass('loading');
    $.get(moreLink.attr('href')).done(function(html) {
      var container = $(document.createElement('div'));
      var newRecords = container.append(html).find('.extension-history-record');
      $(document).trigger('xwiki:dom:updated', {'elements': container.toArray()});
      loadingRecord.replaceWith(newRecords);
    }).fail(function() {
      loadingRecord.removeClass('loading');
      moreLink.css('visibility', null);
      // TODO: Notify the user about the failed request.
    });
  };

  var enableDisableActions = function() {
    var recordsForm = $(this).closest('.extension-history-records-form');
    var selectedRecordsCount = recordsForm.find('input[name="extensionHistoryRecord"]:checked').length;
    recordsForm.find('button').prop('disabled', selectedRecordsCount == 0);
  };

  var enhanceHistoryRecordsForm = function(recordsForm) {
    // Enhance the 'more' link.
    recordsForm.find('.extension-history-record a.more').click(loadMoreHistoryRecords);
    // Enable/Disable the history actions based on the number of records selected.
    enableDisableActions.call(recordsForm);
    recordsForm.find('.extension-history-record input[type="checkbox"]').click(enableDisableActions);
  };

  enhanceHistoryRecordsForm($('.extension-history-records-form'));
  $(document).on('xwiki:dom:updated', function(event, data) {
    enhanceHistoryRecordsForm($(data.elements));
  });

  //
  // Extension History Replay
  //

  var onPreviewReplayPlan = function(event) {
    event.preventDefault();
    var extensionHistory = $(event.target).closest('.extension-history');
    var replayOptions = extensionHistory.find('.extension-history-replay-options');
    if (replayOptions.length > 0) {
      replayOptions.removeClass('hidden').prevAll().hide();
      replayOptions.get(0).scrollIntoView();
    } else {
      getReplayPlan(extensionHistory);
    }
  };

  $('.extension-history-actions button[value="replayPlan"]').click(onPreviewReplayPlan);
  $(document).on('xwiki:dom:updated', function(event, data) {
    $(data.elements).find('.extension-history-actions button[value="replayPlan"]').click(onPreviewReplayPlan);
  });

  $('.extension-history-replay-options button[value="replayPlan"]').click(function(event) {
    event.preventDefault();
    getReplayPlan($(event.target).closest('.extension-history'));
  });

  $('.extension-history-replay-options a.btn-default').click(function(event) {
    event.preventDefault();
    replayPlanRequest && replayPlanRequest.abort();
    $(event.target).closest('.extension-history-replay-options').addClass('hidden').prevAll().show();
  });

  var enhanceReplayPlan = function(replayPlan) {
    replayPlan.find('a.btn-default').click(cancelReplayPlan);
    replayPlan.find('button[value="replay"]').click(submitReplayPlan);
  };

  var replayPlanRequest;
  var getReplayPlan = function(extensionHistory) {
    var forms = extensionHistory.children('form');
    var data = forms.serialize() + '&data=replayPlan';
    forms.find(':input').prop('disabled', true);
    replayPlanRequest = $.post(forms.attr('action'), data).always(function() {
      forms.find(':input').prop('disabled', false);
    }).done(function(html) {
      extensionHistory.children().hide();
      extensionHistory.find('.extension-history-replay-options').show().addClass('hidden');
      extensionHistory.append(html);
      var replayPlan = extensionHistory.find('.extension-history-replay-plan');
      replayPlan.size() > 0 && $(document).trigger('xwiki:dom:updated', {'elements': replayPlan.toArray()});
      enhanceReplayPlan(replayPlan);
    }).fail(function() {
      // TODO
    });
  };

  var cancelReplayPlan = function(event) {
    event.preventDefault();
    var extensionHistory = $(event.target).closest('.extension-history');
    extensionHistory.children('.extension-history-replay-plan').remove();
    extensionHistory.children().show();
  };

  var submitReplayPlan = function(event) {
    event.preventDefault();
    var replayButton = $(this).prop('disabled', true);
    var form = replayButton.closest('form');
    var data = form.serialize() + '&action=replay';
    $.post(form.attr('action'), data).done(function(data) {
      // Make sure we remove the document fragment identifier from the end of the URL.
      var replayStatusURL = window.location.href.replace(/#.*$/, '');
      replayStatusURL += replayStatusURL.indexOf('?') < 0 ? '?' : '&';
      window.location = replayStatusURL + $.param({
        'data': 'replayStatus',
        'jobId': data.jobId
      });
    }).fail(function() {
      replayButton.prop('disabled', false);
      // TODO: Display the error message.
    });
  };

  //
  // Extension History Replay Status
  //

  var enhanceReplayStatus = function(container, uiState) {
    container.find('.extension-history-replay-log .xHint').click(function() {
      $(this).closest('.extension-history-replay-log').toggleClass('opened');
    });

    uiState = uiState || {};
    container.find('.extension-history-replay-log').toggleClass('opened', !!uiState.logOpened);

    var jobState = container.attr('data-jobState');
    if (jobState == 'WAITING') {
      container.find('form.extension-question button[value="continue"]').click($.proxy(updateReplayStatus, null,
        container, 'action=continue'));
      // Some questions have their own buttons (e.g. the "Show changes" button on the merge conflict question).
      container.find('form.extension-question button[name="extensionAction"]').click($.proxy(updateReplayStatus, null,
        container, 'data=replayStatus'));
    } else if (typeof jobState == 'string' && jobState != 'FINISHED') {
      setTimeout($.proxy(updateReplayStatus, this, container), 1000);
    }
  };

  var getReplayStatusUIState = function(container) {
    return {
      'logOpened': container.find('.extension-history-replay-log.opened').size() > 0
    };
  };

  var updateReplayStatus = function(container, data, event) {
    if (event) {
      event.preventDefault();
      data += '&' + $(this).closest('form').serialize();
    } else {
      data = data || {
        'data': 'replayStatus',
        'jobId': container.attr('data-jobId')
      };
    }
    $.post(container.attr('data-extensionHistoryURL'), data).done(function(html) {
      var uiState = getReplayStatusUIState(container);
      var wrapper = $(document.createElement('div'));
      wrapper.append(html);
      enhanceReplayStatus(wrapper.find('.extension-history-replay-status'), uiState);
      var newElements = wrapper.children();
      container.replaceWith(newElements);
      $(document).trigger('xwiki:dom:updated', {'elements': newElements.toArray()});
    }).fail(function() {
      // TODO
    })
  };

  enhanceReplayStatus($('.extension-history-replay-status'));
});
