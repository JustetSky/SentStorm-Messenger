import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
  url: 'http://localhost:9090',
  realm: 'sentstorm',
  clientId: 'sentstorm-app'
})

export default keycloak
