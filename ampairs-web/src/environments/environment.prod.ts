export const environment = {
  production: true,
  apiBaseUrl: 'https://api.ampairs.com', // Update with your production API URL
  recaptcha: {
    siteKey: 'YOUR_PRODUCTION_RECAPTCHA_SITE_KEY' // Replace with your production site key
  },
  firebase: {
    apiKey: 'YOUR_PROD_FIREBASE_API_KEY',
    authDomain: 'YOUR_PROD_PROJECT.firebaseapp.com',
    projectId: 'YOUR_PROD_PROJECT_ID',
    storageBucket: 'YOUR_PROD_PROJECT.appspot.com',
    messagingSenderId: 'YOUR_PROD_SENDER_ID',
    appId: 'YOUR_PROD_APP_ID'
  },
  deepLink: {
    scheme: 'ampairs',
    host: 'auth'
  }
};