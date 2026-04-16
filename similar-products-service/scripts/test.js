const http = require('k6/http');
const { check, sleep } = require('k6');

export let options = {
    vus: 10, // Virtual users
    duration: '30s', // Duration of the test
};

export default function () {
    const productId = 1; // Example product ID
    const url = `http://localhost:5000/products/${productId}/similar`;

    let response = http.get(url);

    check(response, {
        'is status 200': (r) => r.status === 200,
        'response time < 200ms': (r) => r.timings.duration < 200,
    });

    sleep(1); // Sleep for 1 second between requests
}