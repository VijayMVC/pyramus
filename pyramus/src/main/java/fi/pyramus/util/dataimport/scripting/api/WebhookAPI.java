package fi.pyramus.util.dataimport.scripting.api;

import fi.pyramus.dao.DAOFactory;
import fi.pyramus.domainmodel.webhooks.Webhook;

public class WebhookAPI {
  
  public Long create(String url, String secret) {
    Webhook webhook = DAOFactory.getInstance().getWebhookDAO().create(url, secret);
    if (webhook == null) {
      return null;
    } else {
      return webhook.getId();
    }
  }

}
