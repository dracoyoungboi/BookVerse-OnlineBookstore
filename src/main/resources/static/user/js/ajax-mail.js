$(function() {

	// Get the form.
	var form = $('#contact-form');

	// Get the messages div.
	var formMessages = $('.form-messege');

	// Set up an event listener for the contact form.
	$(form).submit(function(e) {
		// Stop the browser from submitting the form.
		e.preventDefault();

		// Clear previous error messages
		$('.error-message').text('');
		$(formMessages).removeClass('error success').text('');

		// Get form values
		var name = $('#name').val().trim();
		var email = $('#email').val().trim();
		var subject = $('#subject').val().trim();
		var message = $('#message').val().trim();

		// Client-side validation
		var errors = [];
		if (!name || name.length < 2) {
			errors.push('Name must be at least 2 characters');
		}
		if (!email || !isValidEmail(email)) {
			errors.push('Please provide a valid email address');
		}
		if (!subject || subject.length < 3) {
			errors.push('Subject must be at least 3 characters');
		}
		if (!message || message.length < 10) {
			errors.push('Message must be at least 10 characters');
		}

		if (errors.length > 0) {
			$(formMessages).removeClass('success').addClass('error');
			$(formMessages).text(errors.join(', '));
			return;
		}

		// Prepare form data (serialize form)
		var formData = $(form).serialize();

		// Disable submit button
		var submitBtn = $(form).find('button[type="submit"]');
		submitBtn.prop('disabled', true).text('SENDING...');

		// Submit the form using AJAX.
		$.ajax({
			type: 'POST',
			url: $(form).attr('action'),
			data: formData,
			success: function(response) {
				// Make sure that the formMessages div has the 'success' class.
				$(formMessages).removeClass('error');
				$(formMessages).addClass('success');

				// Set the message text.
				$(formMessages).text(response);

				// Clear the form.
				$('#contact-form input,#contact-form textarea').val('');
			},
			error: function(xhr) {
				// Make sure that the formMessages div has the 'error' class.
				$(formMessages).removeClass('success');
				$(formMessages).addClass('error');

				// Set the message text.
				var errorMessage = 'Oops! An error occurred and your message could not be sent.';
				if (xhr.responseText) {
					errorMessage = xhr.responseText;
				} else if (xhr.status === 400) {
					errorMessage = 'Please check your input and try again.';
				} else if (xhr.status === 500) {
					errorMessage = 'Server error. Please try again later.';
				}
				$(formMessages).text(errorMessage);
			},
			complete: function() {
				// Re-enable submit button
				submitBtn.prop('disabled', false).text('SEND MESSAGE');
			}
		});
	});

	// Email validation function
	function isValidEmail(email) {
		var emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
		return emailRegex.test(email);
	}

});
