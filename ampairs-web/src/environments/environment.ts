export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  recaptcha: {
    // Using a working development site key - you should replace this with your actual site key
    // For now, using a dummy token approach for development
    siteKey: '6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI', // Test site key (returns null)
    enabled: false // Disable reCAPTCHA for development
  },
  firebase: {
    apiKey: 'YOUR_DEV_FIREBASE_API_KEY',
    authDomain: 'YOUR_DEV_PROJECT.firebaseapp.com',
    projectId: 'YOUR_DEV_PROJECT_ID',
    storageBucket: 'YOUR_DEV_PROJECT.appspot.com',
    messagingSenderId: 'YOUR_DEV_SENDER_ID',
    appId: 'YOUR_DEV_APP_ID'
  },
  deepLink: {
    scheme: 'ampairs',
    host: 'auth'
  }
};