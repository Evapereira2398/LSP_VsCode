package custom.senior.incidente;

import java.util.List;
import java.time.LocalDate;
import java.time.DayOfWeek;

import com.senior.dataset.ICursor;
import com.senior.dataset.MappedParamProvider;
import com.senior.entitysession.EntitySessionFactory;
import com.senior.entitysession.IEntitySession;
import com.senior.rh.entities.readonly.IR009TII;
import com.senior.rh.entities.readonly.IR066SIT;
import com.senior.rh.entities.readonly.IR070ACC;
import com.senior.rh.ponto.marcacoes.MarcacaoRegraIncidente;

import custom.br.com.senior.incidente.ContextoIncidente;
import custom.br.com.senior.incidente.PontoRegraIncidente;

public class RegraIncidente extends PontoRegraIncidente {
	
	private boolean naoTrabalhoudomingoAtual;


	@Override
	public void calcular(ContextoIncidente contextoIncidente) {
		if (retornaMais6Horas(contextoIncidente)) {
			contextoIncidente.criarIncidente(5);
		}
		/*if (retornaAdministrativo(contextoIncidente)) {
			contextoIncidente.criarIncidente(20);
		}*/
		
		//Bloco 3° Domingo trabalhado
			org.joda.time.LocalDate dataApuracao = contextoIncidente.getDataApuracao();
	
	        if (dataApuracao.getDayOfWeek() == 7){
	        	if (terceiroDomingo(contextoIncidente, dataApuracao)) 
	        		contextoIncidente.criarIncidente(98);
	        }
		//Bloco 3° Domingo trabalhado		
		
	}

	
    private boolean terceiroDomingo(ContextoIncidente contextoIncidente, org.joda.time.LocalDate dataApuracao) {
    	org.joda.time.LocalDate domingoAtual = dataApuracao;
    	org.joda.time.LocalDate primeiroDomingoAnterior = dataApuracao.minusWeeks(1);
        org.joda.time.LocalDate segundoDomingoAnterior = dataApuracao.minusWeeks(2);
        
		List<MarcacaoRegraIncidente> mdomingoAtual = contextoIncidente.getMarcacoesPonto(true,domingoAtual);
		List<MarcacaoRegraIncidente> mprimeiroDomingo = contextoIncidente.getMarcacoesPonto(true,primeiroDomingoAnterior);
		List<MarcacaoRegraIncidente> msegundoDomingo = contextoIncidente.getMarcacoesPonto(true,segundoDomingoAnterior);

        boolean trabalhoudomingoAtual = mdomingoAtual.size() > 0;
        boolean trabalhouPrimeiroDomingo = mprimeiroDomingo.size() > 0;
        boolean trabalhouSegundoDomingo = msegundoDomingo.size() > 0;
        
        return trabalhoudomingoAtual && trabalhouPrimeiroDomingo && trabalhouSegundoDomingo;
//      return trabalhoudomingoAtual && trabalhouPrimeiroDomingo && trabalhouSegundoDomingo;
    }	

	private boolean retornaAdministrativo(ContextoIncidente contextoIncidente) {
		if (contextoIncidente.getColaborador().getNumCad() == 13744) {
			if (retornaExisteIncidente9(contextoIncidente)) {
				return false;
			} else {
				if (retornaHorasR070Acc(contextoIncidente, 2) >= 600) {
					return true;
				} else
					return false;
			}
		} else
			return false;
		/*
		 * if ((retornaHoras(contextoIncidente, 1) < 600) &&
		 * (retornaHoras(contextoIncidente, 2) >= 600)) { return true; } else
		 * return false; } else return false;
		 */
	}
	
		private int retornaHorasR070Acc(ContextoIncidente contextoIncidente, int iTipo) {
		int iHora = 0; 
		List<MarcacaoRegraIncidente> marcacoes = contextoIncidente.getMarcacoesPonto(true,
				contextoIncidente.getDataApuracao());
		
		for (int i = marcacoes.size(); i == 1; i--) {
			if((marcacoes.size() == 2) || (marcacoes.size() == 4) || (marcacoes.size() == 6)){
				iHora = iHora + marcacoes.get(i).getHora() - marcacoes.get(i-1).getHora();
			}			
		}
		return iHora;
	}

	private int retornaHoras(ContextoIncidente contextoIncidente, int iTipo) {
		int iHora = 0;
		IEntitySession entitySession = EntitySessionFactory.newSession();
		MappedParamProvider params = new MappedParamProvider();
		params.setParam("iNumEmp", contextoIncidente.getColaborador().getNumEmp());
		params.setParam("iTipCol", contextoIncidente.getColaborador().getTipCol());
		params.setParam("iNumCad", contextoIncidente.getColaborador().getNumCad());
		params.setParam("dDatApu", contextoIncidente.getDataApuracao());
		ICursor<IR066SIT> crR066Sit = entitySession.newCursor(IR066SIT.class);
		if (iTipo == 1) {
			crR066Sit.addFilter(
					"NumEmp = :iNumEmp And TipCol =:iTipCol And NumCad = :iNumCad And DatApu = :dDatApu And CodSit In (1, 16, 51, 66, 301, 302, 303, 304, 305, 306, 401, 402, 403, 404, 405)",
					params);
		} else {
			crR066Sit.addFilter(
					"NumEmp = :iNumEmp And TipCol =:iTipCol And NumCad = :iNumCad And DatApu = :dDatApu And CodSit In (1, 16, 51, 66, 103, 104, 301, 302, 303, 304, 305, 306, 401, 402, 403, 404, 405)",
					params);
		}
		crR066Sit.open();
		try {
			IR066SIT r066sit = crR066Sit.newBuffer();
			while (crR066Sit.next()) {
				crR066Sit.read(r066sit);
				iHora = iHora + r066sit.getQtdHor();
			}
		} finally {
			crR066Sit.close();
			entitySession.close();
		}
		return iHora;
	}

	private boolean retornaExisteIncidente9(ContextoIncidente contextoIncidente) {
		boolean bRetorno = false;
		IEntitySession entitySession = EntitySessionFactory.newSession();
		MappedParamProvider params = new MappedParamProvider();
		params.setParam("iNumEmp", contextoIncidente.getColaborador().getNumEmp());
		params.setParam("iTipCol", contextoIncidente.getColaborador().getTipCol());
		params.setParam("iNumCad", contextoIncidente.getColaborador().getNumCad());
		params.setParam("dDatApu", contextoIncidente.getDataApuracao());
		ICursor<IR009TII> crR009Tii = entitySession.newCursor(IR009TII.class);

		crR009Tii.addFilter(
				"NumEmp = :iNumEmp And TipCol =:iTipCol And NumCad = :iNumCad And DatApi = :dDatApu And CodTAl = 9 ",
				params);

		crR009Tii.open();
		try {
			while (crR009Tii.next()) {
				bRetorno = true;
			}
		} finally {
			crR009Tii.close();
			entitySession.close();
		}
		return bRetorno;
	}


	private boolean retornaMais6Horas(ContextoIncidente contextoIncidente) {

		int iHorTra = 0;
		int horarios[] = new int[9];

		List<MarcacaoRegraIncidente> marcacoes = contextoIncidente.getMarcacoesPonto(true,
				contextoIncidente.getDataApuracao());

		if (marcacoes.size() == 0) {

			IEntitySession entitySession = EntitySessionFactory.newSession();
			try {
				MappedParamProvider params = new MappedParamProvider();

				params.setParam("iNumEmp", contextoIncidente.getColaborador().getNumEmp());
				params.setParam("iTipCol", contextoIncidente.getColaborador().getTipCol());
				params.setParam("iNumCad", contextoIncidente.getColaborador().getNumCad());
				params.setParam("dDatApu", contextoIncidente.getDataApuracao());

				ICursor<IR070ACC> c070Acc = entitySession.newCursor(IR070ACC.class);
				int index = 0;
				int horAcc = 0;

				while (index < 4) {
					params.setParam("index", index);
					c070Acc.addFilter(
							"NumEmp = :iNumEmp And TipCol =:iTipCol And NumCad = :iNumCad And DatApu = :dDatApu And SeqAcc = :index",
							params);
					c070Acc.open();
					IR070ACC ir070Acc = c070Acc.newBuffer();
					if (!ir070Acc.isHorAccNull())
						horAcc = ir070Acc.getHorAcc();
					horarios[index] = (int) horAcc;
					index++;
					c070Acc.close();
				}
				
				
				if (horarios.length == 4) {
					return true;
				} else {
					return false;
				}
			} finally {
				entitySession.close();
			}

		} else {

			if(marcacoes.size() == 4) {
				iHorTra = marcacoes.get(1).getHora() - marcacoes.get(0).getHora();
				if (iHorTra <= 360)
					iHorTra = marcacoes.get(3).getHora() - marcacoes.get(2).getHora();
			} else if (marcacoes.size() == 6) {
				iHorTra = marcacoes.get(3).getHora() - marcacoes.get(2).getHora();
			}

			if (iHorTra > 360)
				return true;
			else
				return false;
		}
	}
}