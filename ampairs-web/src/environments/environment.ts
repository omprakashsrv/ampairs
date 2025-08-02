export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  recaptcha: {
    // Using a working development site key - you should replace this with your actual site key
    // For now, using a dummy token approach for development
    siteKey: '6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI', // Test site key (returns null)
    enabled: false // Disable reCAPTCHA for development
  }
};